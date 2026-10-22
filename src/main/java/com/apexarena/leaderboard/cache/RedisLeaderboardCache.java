package com.apexarena.leaderboard.cache;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisZSetCommands.ZAddArgs;
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

/**
 * Leaderboards as Redis sorted sets: one ZSET per board, member = player, score = best points.
 *
 * <ul>
 *   <li><b>Write-through:</b> after a score is committed, {@code ZADD key GT points player}
 *       updates each of its boards. GT keeps the higher value, so updates can arrive in any
 *       order (or twice) and the result is still each player's maximum.</li>
 *   <li><b>Lazy load (cache-aside):</b> a board is loaded from MySQL the first time it is read.
 *       A sentinel member marks the set as complete; until it is present, readers use MySQL.
 *       Because loading also uses ZADD GT, a write racing with a load is never lost.</li>
 *   <li><b>TTL:</b> complete boards expire after {@code leaderboard.cache.ttl} and are reloaded,
 *       which heals any drift (e.g. writes missed while Redis was down).</li>
 *   <li><b>Fail-open:</b> on any Redis error we skip Redis for a few seconds (a tiny circuit
 *       breaker) and answer from MySQL, so a Redis outage only costs speed.</li>
 *   <li><b>Recovery:</b> if a write could not reach Redis, the cache is marked stale and all
 *       boards are deleted on the next successful call, so they reload from MySQL instead of
 *       serving a board that is missing that score.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "leaderboard.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisLeaderboardCache implements LeaderboardCache {

    private static final Logger log = LoggerFactory.getLogger(RedisLeaderboardCache.class);

    static final String KEY_PREFIX = "lb:";
    /** Marks a fully loaded board. Score -1 sorts it below every real player (points >= 0). */
    static final String SENTINEL = "\u0000complete";

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
            // ZREVRANGE key 0 n-1 WITHSCORES  (highest first)
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
            Double best = redis.opsForZSet().score(key, playerName);           // ZSCORE
            Long size = redis.opsForZSet().zCard(key);                          // ZCARD
            long total = size == null ? 0 : Math.max(0, size - 1);              // minus sentinel
            if (best == null) {
                return new CachedRank(null, 0, total);
            }
            // ZCOUNT key (best +inf  -> players strictly above (points are integers)
            Long above = redis.opsForZSet().count(key, best + 1, Double.POSITIVE_INFINITY);
            return new CachedRank(best.intValue(), above == null ? 0 : above, total);
        });
    }

    @Override
    public void record(Score score) {
        guarded(() -> {
            byte[] member = bytes(score.playerName());
            ZAddArgs gt = ZAddArgs.empty().gt();
            // One round trip for all three boards.
            redis.executePipelined((RedisCallback<Object>) conn -> {
                for (Board b : Board.boardsFor(score)) {
                    conn.zSetCommands().zAdd(bytes(key(b)), score.points(), member, gt);
                }
                return null;
            });
            return Boolean.TRUE;
        }).ifPresentOrElse(ok -> { }, () -> stale.set(true));
    }

    /** True when the board's ZSET is complete; loads it from the database if needed. */
    private boolean ensureLoaded(Board board) {
        String key = key(board);
        if (redis.opsForZSet().score(key, SENTINEL) != null) {
            return true;                                                    // cache hit
        }
        // Cache miss. Only one thread per board loads; the others use MySQL meanwhile
        // instead of all hammering the database with the same query (cache stampede).
        ReentrantLock lock = loadLocks.computeIfAbsent(key, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            return false;
        }
        try {
            if (redis.opsForZSet().score(key, SENTINEL) != null) {
                return true;
            }
            List<PlayerBest> all = repository.allPlayers(board);
            ZAddArgs gt = ZAddArgs.empty().gt();
            byte[] k = bytes(key);
            redis.executePipelined((RedisCallback<Object>) conn -> {
                for (PlayerBest p : all) {
                    conn.zSetCommands().zAdd(k, p.points(), bytes(p.playerName()), gt);
                }
                conn.zSetCommands().zAdd(k, -1, bytes(SENTINEL));
                conn.keyCommands().expire(k, ttl.toSeconds());
                return null;
            });
            log.info("Loaded leaderboard {} into Redis ({} players)", key, all.size());
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Runs a Redis action; on failure opens the breaker and returns empty (= use the database). */
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
