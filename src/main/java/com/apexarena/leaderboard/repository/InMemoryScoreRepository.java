package com.apexarena.leaderboard.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.apexarena.leaderboard.domain.Score;

@Repository
public class InMemoryScoreRepository implements ScoreRepository {

    private final List<Score> scores = new CopyOnWriteArrayList<>();
    // player -> best points; merge(..., Math::max) is atomic per key
    private final Map<String, Integer> bestByPlayer = new ConcurrentHashMap<>();

    @Override
    public Score save(Score score) {
        scores.add(score);
        bestByPlayer.merge(score.playerName(), score.points(), Math::max);
        return score;
    }

    @Override
    public List<PlayerBest> topPlayers(int limit) {
        return bestByPlayer.entrySet().stream()
                .map(e -> new PlayerBest(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(PlayerBest::points).reversed()
                        .thenComparing(PlayerBest::playerName))
                .limit(limit)
                .toList();
    }
}
