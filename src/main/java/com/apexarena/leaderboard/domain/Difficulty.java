package com.apexarena.leaderboard.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** AI difficulty in Apex Arena. Each one carries the base points for a win. */
public enum Difficulty {
    EASY(100),
    HARD(250),
    BOSS(500);

    private final int basePoints;

    Difficulty(int basePoints) {
        this.basePoints = basePoints;
    }

    public int basePoints() {
        return basePoints;
    }

    /** The game uses lowercase names ("easy", "hard", "boss"), so JSON does too. */
    @JsonValue
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static Difficulty from(String value) {
        if (value != null) {
            for (Difficulty d : values()) {
                if (d.name().equalsIgnoreCase(value.trim())) {
                    return d;
                }
            }
        }
        throw new IllegalArgumentException("Unknown difficulty '" + value + "', expected easy, hard or boss");
    }
}
