package com.apexarena.leaderboard.service;

import org.springframework.stereotype.Component;

import com.apexarena.leaderboard.config.GameProperties;
import com.apexarena.leaderboard.domain.Difficulty;

/**
 * The scoring rules, kept in one pure class so they are easy to unit test and tune.
 *
 * <pre>
 *   loss:  base / 10                       (a little credit for trying)
 *   win:   base                            easy 100, hard 250, boss 500
 *        + base * hpLeft%                  up to +100% for a flawless win
 *        + base * 0.5 * timeLeft / length  up to +50% for a fast win
 * </pre>
 */
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
