package com.apexarena.leaderboard.cache;

import com.apexarena.leaderboard.domain.Score;

public record ScoreSubmittedEvent(Score score) {}
