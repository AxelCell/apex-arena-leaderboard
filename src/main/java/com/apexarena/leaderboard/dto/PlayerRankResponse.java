package com.apexarena.leaderboard.dto;

/** Where a player stands on one board. Rank 1 is the top; ties share a rank. */
public record PlayerRankResponse(String playerName, String board, int rank, int bestPoints, long totalPlayers) {}
