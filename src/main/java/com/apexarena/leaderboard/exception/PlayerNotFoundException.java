package com.apexarena.leaderboard.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerName, String board) {
        super("No scores for player '" + playerName + "' on board '" + board + "'");
    }
}
