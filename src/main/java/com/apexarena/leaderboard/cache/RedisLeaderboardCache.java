package com.apexarena.leaderboard.cache;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisZSetCommands.ZAddArgs;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.connection.zset.Tuple;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.repository.PlayerBest;
import com.apexarena.leaderboard.repository.ScoreRepository;

@Component
@ConditionalOnProperty(name = "leaderboard.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisLeaderboardCache implements LeaderboardCache {
    private static final Logger log = LoggerFactory.getLogger(RedisLeaderboardCache.class);

    static final String KEY_PREFIX = "lb:";
    static final String SENTINEL = "\u0000complete";

    private static final int LOAD_BATCH = 1000;
    private static final String LOAD_PREFIX = "lbload:";

    private final StringRedisTemplate redis;
    private final ScoreRepository repository;
    private final Clock clock;
    private final Duration ttl;
    private final Duration breakerOpenFor;

    private final Map<String, ReentrantLock> loadLocks = new ConcurrentHashMap<>();
    private volatile long skipRedisUntilMillis = 0;
    private final AtomicBoolean stale = new AtomicBoolean(false);

    public RedisLeaderboardCache(StringRedisTemplate redis, ScoreRepository repository, Clock clock,
                                 @Value("${leaderboard.cache.ttl:10m}") Duration ttl,
                                 @Value("${leaderboard.cache.breaker-open-for:5s}") Duration breakerOpenFor) {
        this.redis = redis;
        this.repository = repository;
        this.clock = clock;
        this.ttl = ttl;
        this.breakerOpenFor = breakerOpenFor;
    }

    static String key(Board board) {
        return KEY_PREFIX + board.key();
    }

    @Override
    public Optional<List<PlayerBest>> top(Board board, int n) {
        return guarded(() -> {
            String key = key(board);
            if (!ensureLoaded(board)) {
                return null;
            }
            Set<TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, 0, n - 1);
            List<PlayerBest> out = new ArrayList<>();
            if (tuples != null) {
                for (TypedTuple<String> t : tuples) {
                    if (!SENTINEL.equals(t.getValue()) && t.getScore() != null) {
                        out.add(new PlayerBest(t.getValue(), t.getScore().intValue()));
                    }
                }
            }
            return out;
        });
    }

    @Override
    public Optional<CachedRank> rank(Board board, String playerName) {
        return guarded(() -> {
            String key = key(board);
            if (!ensureLoaded(board)) {
                return null;
            }
            Double best = redis.opsForZSet().score(key, playerName);
            Long size = redis.opsForZSet().zCard(key);
            long total = size == null ? 0 : Math.max(0, size - 1);
            if (best == null) {
                return new CachedRank(null, 0, total);
            }
            Long above = redis.opsForZSet().count(key, best + 1, Double.POSITIVE_INFINITY);
            return new CachedRank(best.intValue(), above == null ? 0 : above, total);
        });
    }

    @Override
    public void record(Score score) {
        guarded(() -> {
            byte[] member = bytes(score.playerName());
            ZAddArgs gt = ZAddArgs.empty().gt();
            redis.executePipelined((RedisCallback<Object>) conn -> {
                for (Board b : Board.boardsFor(score)) {
                    conn.zSetCommands().zAdd(bytes(key(b)), score.points(), member, gt);
                }
                return null;
            });
            return Boolean.TRUE;
        }).ifPresentOrElse(ok -> { }, () -> stale.set(true));
    }

    private boolean ensureLoaded(Board board) {
        String key = key(board);
        if (redis.opsForZSet().score(key, SENTINEL) != null) {
            return true;
        }
        ReentrantLock lock = loadLocks.computeIfAbsent(key, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            return false;
        }
        try {
            if (redis.opsForZSet().score(key, SENTINEL) != null) {
                return true;
            }
            List<PlayerBest> all = repository.allPlayers(board);
            String tmp = LOAD_PREFIX + key + ":" + UUID.randomUUID();
            byte[] t = bytes(tmp);
            redis.execute((RedisCallback<Object>) conn -> {
                for (int i = 0; i < all.size(); i += LOAD_BATCH) {
                    Set<Tuple> batch = new HashSet<>();
                    for (PlayerBest p : all.subList(i, Math.min(all.size(), i + LOAD_BATCH))) {
                        batch.add(new DefaultTuple(bytes(p.playerName()), (double) p.points()));
                    }
                    conn.zSetCommands().zAdd(t, batch, ZAddArgs.empty());
                }
                conn.zSetCommands().zAdd(t, -1, bytes(SENTINEL));
                conn.keyCommands().expire(t, 60);
                return null;
            });
            redis.opsForZSet().unionAndStore(key, List.of(tmp), key, Aggregate.MAX);
            redis.expire(key, ttl);
            redis.delete(tmp);
            log.info("Loaded leaderboard {} into Redis ({} players)", key, all.size());
            return redis.opsForZSet().score(key, SENTINEL) != null;
        } finally {
            lock.unlock();
        }
    }

    private <T> Optional<T> guarded(Supplier<T> action) {
        long now = clock.millis();
        if (now < skipRedisUntilMillis) {
            return Optional.empty();
        }
        try {
            if (stale.getAndSet(false)) {
                invalidateAll();
            }
            return Optional.ofNullable(action.get());
        } catch (RuntimeException e) {
            skipRedisUntilMillis = now + breakerOpenFor.toMillis();
            log.warn("Redis unavailable, using the database for {}: {}", breakerOpenFor, e.toString());
            return Optional.empty();
        }
    }

    private void invalidateAll() {
        try {
            List<String> keys = new ArrayList<>();
            try (Cursor<String> c = redis.scan(ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(500).build())) {
                c.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
            log.info("Redis reachable again; invalidated {} leaderboard keys", keys.size());
        } catch (RuntimeException e) {
            stale.set(true);
            throw e;
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
