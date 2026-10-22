package com.apexarena.leaderboard.cache;

import com.apexarena.leaderboard.domain.Score;

/** Published inside the submit transaction; handled only after it commits. */
public record ScoreSubmittedEvent(Score score) {}
