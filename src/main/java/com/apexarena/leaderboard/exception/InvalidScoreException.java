package com.apexarena.leaderboard.exception;

/** The request is well-formed JSON but describes a result that cannot happen in the game. */
public class InvalidScoreException extends RuntimeException {

    public InvalidScoreException(String message) {
        super(message);
    }
}
