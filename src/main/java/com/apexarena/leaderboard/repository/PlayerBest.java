package com.apexarena.leaderboard.repository;

/** A player's best points on one board. Also used as a JPQL constructor projection. */
public record PlayerBest(String playerName, int points) {

    public PlayerBest(String playerName, Integer points) {
        this(playerName, points.intValue());
    }
}
