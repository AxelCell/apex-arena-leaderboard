package com.apexarena.leaderboard.domain;

import java.time.Instant;

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
