package com.apexarena.leaderboard.dto;

import java.time.Instant;
import java.util.UUID;

import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.Score;

/** What the game gets back after submitting: the points earned and the new overall rank. */
public record ScoreResponse(
        UUID id,
        String playerName,
        String monster,
        Difficulty difficulty,
        boolean won,
        int points,
        int bestPoints,
        int overallRank,
        Instant submittedAt
) {
    public static ScoreResponse from(Score s, int bestPoints, int overallRank) {
        return new ScoreResponse(s.id(), s.playerName(), s.monster(), s.difficulty(), s.won(),
                s.points(), bestPoints, overallRank, s.submittedAt());
    }
}
