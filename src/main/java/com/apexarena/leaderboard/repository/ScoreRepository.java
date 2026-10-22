package com.apexarena.leaderboard.repository;

import java.util.List;
import java.util.Optional;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;

public interface ScoreRepository {
    Score save(Score score);

    boolean existsByFightId(String fightId);

    List<PlayerBest> topPlayers(Board board, int limit);

    List<PlayerBest> allPlayers(Board board);

    Optional<Integer> bestPoints(Board board, String playerName);

    long countPlayersAbove(Board board, int points);

    long countPlayers(Board board);
}
