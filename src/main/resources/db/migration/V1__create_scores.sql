CREATE TABLE scores (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_name     VARCHAR(16)  NOT NULL,
    monster         VARCHAR(32)  NOT NULL,
    difficulty      VARCHAR(8)   NOT NULL,
    game_mode       VARCHAR(8)   NOT NULL,
    won             BOOLEAN      NOT NULL,
    hp_left_percent INT          NOT NULL,
    fight_seconds   DOUBLE       NOT NULL,
    points          INT          NOT NULL,
    submitted_at    TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_scores_player     ON scores (player_name, points);
CREATE INDEX idx_scores_difficulty ON scores (difficulty, player_name, points);
CREATE INDEX idx_scores_monster    ON scores (monster, player_name, points);
