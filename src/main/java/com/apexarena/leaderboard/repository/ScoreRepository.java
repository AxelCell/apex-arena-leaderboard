package com.apexarena.leaderboard.repository;

import java.util.List;
import java.util.Optional;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;

/**
 * Storage abstraction the service depends on. The JPA implementation lives in
 * {@link JpaScoreRepository}; the service never sees JPA entities or SQL.
 */
public interface ScoreRepository {

    Score save(Score score);

    boolean existsByFightId(String fightId);

    /** Each player's single best score on the board, highest first, at most {@code limit} rows. */
    List<PlayerBest> topPlayers(Board board, int limit);

    /** Every player's best on the board (used to rebuild a cache). */
    List<PlayerBest> allPlayers(Board board);

    Optional<Integer> bestPoints(Board board, String playerName);

    /** How many players have a best score strictly greater than {@code points}. */
    long countPlayersAbove(Board board, int points);

    long countPlayers(Board board);
}
