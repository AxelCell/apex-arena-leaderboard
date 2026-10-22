package com.apexarena.leaderboard.domain;

import java.time.Instant;
import java.util.UUID;

/** One finished fight. Immutable: a score is never edited after it is recorded. */
public record Score(
        UUID id,
        String playerName,
        String monster,
        Difficulty difficulty,
        GameMode mode,
        boolean won,
        int hpLeftPercent,
        double fightSeconds,
        int points,
        Instant submittedAt
) {}
