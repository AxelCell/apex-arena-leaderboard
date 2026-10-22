package com.apexarena.leaderboard.cache;

import java.util.List;
import java.util.Optional;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.repository.PlayerBest;

/**
 * A fast read path for leaderboards. Every method may return {@link Optional#empty()},
 * which means "cache not available, ask the database" - the cache is never required
 * for correctness, only for speed.
 */
public interface LeaderboardCache {

    Optional<List<PlayerBest>> top(Board board, int n);

    Optional<CachedRank> rank(Board board, String playerName);

    /** Called after a score is committed to the database. */
    void record(Score score);

    /**
     * @param bestPoints the player's best on the board, or null if they have no score there
     */
    record CachedRank(Integer bestPoints, long playersAbove, long totalPlayers) {}
}
