package com.apexarena.leaderboard.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
