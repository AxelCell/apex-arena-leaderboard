package com.apexarena.leaderboard.repository;

import java.util.List;

import com.apexarena.leaderboard.domain.Score;

/**
 * Storage abstraction. The service depends on this interface, not on a concrete
 * store, so the in-memory version can later be swapped for a database one.
 */
public interface ScoreRepository {

    Score save(Score score);

    /** Each player's single best score, highest first, at most {@code limit} rows. */
    List<PlayerBest> topPlayers(int limit);

    record PlayerBest(String playerName, int points) {}
}
