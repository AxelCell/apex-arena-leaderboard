package com.apexarena.leaderboard.domain;

import java.time.Instant;
import java.util.UUID;

/** One submitted result. Immutable: a score is never edited after it is recorded. */
public record Score(UUID id, String playerName, int points, Instant submittedAt) {}
