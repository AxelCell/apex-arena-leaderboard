package com.apexarena.leaderboard.repository;

import java.util.List;
import java.util.Optional;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;

/**
 * Storage abstraction. The service depends on this interface, not on a concrete
 * store, so the in-memory version can later be swapped for a database one.
 */
public interface ScoreRepository {

    Score save(Score score);

    /** Each player's single best score on the board, highest first, at most {@code limit} rows. */
    List<PlayerBest> topPlayers(Board board, int limit);

    Optional<Integer> bestPoints(Board board, String playerName);

    /** How many players have a best score strictly greater than {@code points}. */
    long countPlayersAbove(Board board, int points);

    long countPlayers(Board board);

    record PlayerBest(String playerName, int points) {}
}
