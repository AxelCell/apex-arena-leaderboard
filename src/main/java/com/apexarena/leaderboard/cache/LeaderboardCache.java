package com.apexarena.leaderboard.cache;

import java.util.List;
import java.util.Optional;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.repository.PlayerBest;

public interface LeaderboardCache {
    Optional<List<PlayerBest>> top(Board board, int n);

    Optional<CachedRank> rank(Board board, String playerName);

    void record(Score score);

    record CachedRank(Integer bestPoints, long playersAbove, long totalPlayers) {}
}
