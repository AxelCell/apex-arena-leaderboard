CREATE TABLE player_best (
    board_key   VARCHAR(48) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    best_points INT         NOT NULL,
    PRIMARY KEY (board_key, player_name)
);

CREATE INDEX idx_player_best_rank ON player_best (board_key, best_points DESC, player_name);

INSERT INTO player_best (board_key, player_name, best_points)
SELECT 'overall', player_name, MAX(points) FROM scores GROUP BY player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('difficulty:', LOWER(difficulty)), player_name, MAX(points) FROM scores GROUP BY difficulty, player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('monster:', monster), player_name, MAX(points) FROM scores GROUP BY monster, player_name;

DROP INDEX idx_scores_difficulty ON scores;
DROP INDEX idx_scores_monster ON scores;
