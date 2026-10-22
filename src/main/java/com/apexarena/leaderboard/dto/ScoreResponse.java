package com.apexarena.leaderboard.dto;

import java.time.Instant;
import java.util.UUID;

import com.apexarena.leaderboard.domain.Score;

public record ScoreResponse(UUID id, String playerName, int points, Instant submittedAt) {

    public static ScoreResponse from(Score s) {
        return new ScoreResponse(s.id(), s.playerName(), s.points(), s.submittedAt());
    }
}
