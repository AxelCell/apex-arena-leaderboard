package com.apexarena.leaderboard.service;

import org.springframework.stereotype.Component;

import com.apexarena.leaderboard.config.GameProperties;
import com.apexarena.leaderboard.domain.Difficulty;

@Component
public class ScoreCalculator {
    private final int fightLength;

    public ScoreCalculator(GameProperties props) {
        this.fightLength = props.fightLengthSeconds();
    }

    public int points(Difficulty difficulty, boolean won, int hpLeftPercent, double fightSeconds) {
        int base = difficulty.basePoints();
        if (!won) {
            return base / 10;
        }
        int hp = Math.clamp(hpLeftPercent, 0, 100);
        double secs = Math.clamp(fightSeconds, 0.0, fightLength);
        long hpBonus = Math.round(base * hp / 100.0);
        long speedBonus = Math.round(base * 0.5 * (fightLength - secs) / fightLength);
        return (int) (base + hpBonus + speedBonus);
    }
}
