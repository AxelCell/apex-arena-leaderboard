package com.apexarena.leaderboard.cache;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.repository.PlayerBest;

@Component
@ConditionalOnProperty(name = "leaderboard.cache.enabled", havingValue = "false")
public class NoOpLeaderboardCache implements LeaderboardCache {
    @Override
    public Optional<List<PlayerBest>> top(Board board, int n) {
        return Optional.empty();
    }

    @Override
    public Optional<CachedRank> rank(Board board, String playerName) {
        return Optional.empty();
    }

    @Override
    public void record(Score score) {
    }
}
