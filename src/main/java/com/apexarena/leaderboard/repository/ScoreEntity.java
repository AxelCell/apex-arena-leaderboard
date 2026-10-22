package com.apexarena.leaderboard.repository;

import java.time.Instant;

import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.GameMode;
import com.apexarena.leaderboard.domain.Score;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scores")
public class ScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 16)
    private String playerName;

    @Column(nullable = false, length = 32)
    private String monster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, length = 8)
    private GameMode mode;

    @Column(nullable = false)
    private boolean won;

    @Column(name = "hp_left_percent", nullable = false)
    private int hpLeftPercent;

    @Column(name = "fight_seconds", nullable = false)
    private double fightSeconds;

    @Column(nullable = false)
    private int points;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "fight_id", length = 36, unique = true)
    private String fightId;

    protected ScoreEntity() {
    }

    static ScoreEntity from(Score s) {
        ScoreEntity e = new ScoreEntity();
        e.id = s.id();
        e.playerName = s.playerName();
        e.monster = s.monster();
        e.difficulty = s.difficulty();
        e.mode = s.mode();
        e.won = s.won();
        e.hpLeftPercent = s.hpLeftPercent();
        e.fightSeconds = s.fightSeconds();
        e.points = s.points();
        e.submittedAt = s.submittedAt();
        e.fightId = s.fightId();
        return e;
    }

    Score toDomain() {
        return new Score(id, playerName, monster, difficulty, mode, won, hpLeftPercent, fightSeconds, points,
                submittedAt, fightId);
    }
}
