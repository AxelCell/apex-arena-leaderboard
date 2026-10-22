package com.apexarena.leaderboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.apexarena.leaderboard.dto.SubmitScoreRequest;
import com.apexarena.leaderboard.exception.InvalidScoreException;

/**
 * Server-side sanity checks on a fight result. Bean Validation already checked ranges;
 * these rules check that the combination of values could really happen in the game.
 */
@Component
public class ScoreValidator {

    private final double minWinSeconds;

    public ScoreValidator(@Value("${leaderboard.anticheat.min-win-seconds:3}") double minWinSeconds) {
        this.minWinSeconds = minWinSeconds;
    }

    public void validate(SubmitScoreRequest req) {
        if (req.won() && req.hpLeftPercent() == 0) {
            throw new InvalidScoreException("A winner cannot have 0 HP left");
        }
        if (req.won() && req.fightSeconds() < minWinSeconds) {
            throw new InvalidScoreException("A win in under " + minWinSeconds + " seconds is not possible");
        }
    }
}
