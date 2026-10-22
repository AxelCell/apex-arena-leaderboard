package com.apexarena.leaderboard.domain;

import java.util.Locale;

/**
 * Which leaderboard we are talking about. Every score lands on three boards:
 * overall, its difficulty, and the monster the player used.
 */
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

    /** Build a board from optional query params; at most one filter may be given. */
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

    /** All boards a score counts towards. */
    public static Board[] boardsFor(Score s) {
        return new Board[] { overall(), of(s.difficulty()), ofMonster(s.monster()) };
    }

    /** Stable name, e.g. "overall", "difficulty:hard", "monster:zapret". */
    public String key() {
        return type == Type.OVERALL ? "overall" : type.name().toLowerCase(Locale.ROOT) + ":" + value;
    }
}
