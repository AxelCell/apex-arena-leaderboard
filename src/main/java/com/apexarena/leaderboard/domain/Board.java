package com.apexarena.leaderboard.domain;

import java.util.Locale;

public record Board(Type type, String value) {
    public enum Type { OVERALL, DIFFICULTY, MONSTER }

    public static Board overall() {
        return new Board(Type.OVERALL, null);
    }

    public static Board of(Difficulty d) {
        return new Board(Type.DIFFICULTY, d.key());
    }

    public static Board ofMonster(String monster) {
        return new Board(Type.MONSTER, monster.toLowerCase(Locale.ROOT));
    }

    public static Board from(String difficulty, String monster) {
        boolean hasDiff = difficulty != null && !difficulty.isBlank();
        boolean hasMonster = monster != null && !monster.isBlank();
        if (hasDiff && hasMonster) {
            throw new IllegalArgumentException("Use either 'difficulty' or 'monster', not both");
        }
        if (hasDiff) {
            return of(Difficulty.from(difficulty));
        }
        if (hasMonster) {
            return ofMonster(monster.trim());
        }
        return overall();
    }

    public static Board[] boardsFor(Score s) {
        return new Board[] { overall(), of(s.difficulty()), ofMonster(s.monster()) };
    }

    public String key() {
        return type == Type.OVERALL ? "overall" : type.name().toLowerCase(Locale.ROOT) + ":" + value;
    }
}
