package com.apexarena.leaderboard.dto;

/** One row of a leaderboard: a player's best score and their position. */
public record LeaderboardEntry(int rank, String playerName, int points) {}
