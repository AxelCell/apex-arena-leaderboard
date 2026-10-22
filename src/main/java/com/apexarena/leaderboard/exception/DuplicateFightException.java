package com.apexarena.leaderboard.exception;

/** The same fightId was submitted twice (a replay or a client retry). */
public class DuplicateFightException extends RuntimeException {

    public DuplicateFightException(String fightId) {
        super("Fight " + fightId + " was already submitted");
    }
}
