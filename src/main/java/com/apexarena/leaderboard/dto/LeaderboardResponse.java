package com.apexarena.leaderboard.dto;

import java.util.List;

public record LeaderboardResponse(String board, List<LeaderboardEntry> entries) {}
