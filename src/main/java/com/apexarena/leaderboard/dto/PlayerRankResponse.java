package com.apexarena.leaderboard.dto;

public record PlayerRankResponse(String playerName, String board, int rank, int bestPoints, long totalPlayers) {}
