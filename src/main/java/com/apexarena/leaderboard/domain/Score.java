package com.apexarena.leaderboard.domain;

import java.time.Instant;

/**
 * One finished fight. Immutable: a score is never edited after it is recorded.
 * {@code id} is null until the database assigns one.
 */
public record Score(
        Long id,
        String playerName,
        String monster,
        Difficulty difficulty,
        GameMode mode,
        boolean won,
        int hpLeftPercent,
        double fightSeconds,
        int points,
        Instant submittedAt,
        String fightId
) {
    public Score withId(Long newId) {
        return new Score(newId, playerName, monster, difficulty, mode, won, hpLeftPercent, fightSeconds,
                points, submittedAt, fightId);
    }
}
