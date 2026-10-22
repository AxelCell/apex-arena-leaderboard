package com.apexarena.leaderboard.config;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Facts about the game the server needs, bound from {@code game.*} in application.properties.
 *
 * @param monsters            every valid monster key in Apex Arena
 * @param fightLengthSeconds  length of the fight timer (the game uses 99)
 */
@ConfigurationProperties(prefix = "game")
public record GameProperties(List<String> monsters, int fightLengthSeconds) {

    public GameProperties {
        monsters = monsters == null ? List.of() : List.copyOf(monsters);
        if (fightLengthSeconds <= 0) {
            fightLengthSeconds = 99;
        }
    }

    public Set<String> monsterSet() {
        return monsters.stream().map(m -> m.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
