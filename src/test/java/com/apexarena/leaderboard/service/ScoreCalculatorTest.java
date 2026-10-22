package com.apexarena.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.apexarena.leaderboard.config.GameProperties;
import com.apexarena.leaderboard.domain.Difficulty;

class ScoreCalculatorTest {

    private final ScoreCalculator calc = new ScoreCalculator(new GameProperties(List.of("zapret"), 99));

    @ParameterizedTest(name = "{0} won={1} hp={2}% {3}s -> {4}")
    @CsvSource({
            // difficulty, won, hp%, seconds, expected
            "EASY, true, 100, 10, 245",   // 100 + 100 + round(50 * 89/99)
            "HARD, true,  80, 30, 537",   // 250 + 200 + round(125 * 69/99)
            "BOSS, true,  60, 45, 936",   // 500 + 300 + round(250 * 54/99)
            "BOSS, true,   1, 99, 505",   // slowest possible win: no speed bonus
            "EASY, false,  0, 50,  10",   // loss = base / 10
            "BOSS, false, 90, 99,  50",   // a loss ignores HP and time
    })
    void computesPoints(Difficulty d, boolean won, int hp, double secs, int expected) {
        assertThat(calc.points(d, won, hp, secs)).isEqualTo(expected);
    }

    @Test
    void harderDifficultyAlwaysPaysMoreForTheSameFight() {
        int easy = calc.points(Difficulty.EASY, true, 50, 40);
        int hard = calc.points(Difficulty.HARD, true, 50, 40);
        int boss = calc.points(Difficulty.BOSS, true, 50, 40);
        assertThat(easy).isLessThan(hard);
        assertThat(hard).isLessThan(boss);
    }

    @Test
    void fasterAndHealthierWinsScoreHigher() {
        assertThat(calc.points(Difficulty.HARD, true, 50, 20))
                .isGreaterThan(calc.points(Difficulty.HARD, true, 50, 60));
        assertThat(calc.points(Difficulty.HARD, true, 90, 40))
                .isGreaterThan(calc.points(Difficulty.HARD, true, 10, 40));
    }

    @Test
    void clampsOutOfRangeInput() {
        assertThat(calc.points(Difficulty.EASY, true, 500, -5)).isEqualTo(calc.points(Difficulty.EASY, true, 100, 0));
    }
}
