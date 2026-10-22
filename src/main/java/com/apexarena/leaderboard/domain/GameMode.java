package com.apexarena.leaderboard.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GameMode {
    QUICK,
    LEAGUE;

    @JsonValue
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static GameMode from(String value) {
        if (value != null) {
            for (GameMode m : values()) {
                if (m.name().equalsIgnoreCase(value.trim())) {
                    return m;
                }
            }
        }
        throw new IllegalArgumentException("Unknown mode '" + value + "', expected quick or league");
    }
}
