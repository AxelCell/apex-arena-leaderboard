package com.apexarena.leaderboard.exception;

public class DuplicateFightException extends RuntimeException {
    public DuplicateFightException(String fightId) {
        super("Fight " + fightId + " was already submitted");
    }
}
