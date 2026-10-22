package com.apexarena.leaderboard.dto;

import java.util.List;

/** A board name plus its top entries, e.g. board = "difficulty:boss". */
public record LeaderboardResponse(String board, List<LeaderboardEntry> entries) {}
