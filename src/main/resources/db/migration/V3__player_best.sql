-- Materialized "best score per player per board". Before this, every leaderboard read ran
-- GROUP BY player_name over the whole scores table (~440 ms with 200k rows). Now each
-- submission upserts 3 rows here (overall, difficulty, monster) and reads are index scans.
CREATE TABLE player_best (
    board_key   VARCHAR(48) NOT NULL,   -- "overall", "difficulty:hard", "monster:zapret"
    player_name VARCHAR(16) NOT NULL,
    best_points INT         NOT NULL,
    PRIMARY KEY (board_key, player_name)
);

-- Top-N reads this index in order (best first, ties by name) and stops after N rows;
-- rank = COUNT(*) of the range "best_points > mine" on the same index.
CREATE INDEX idx_player_best_rank ON player_best (board_key, best_points DESC, player_name);

-- Backfill from existing scores.
INSERT INTO player_best (board_key, player_name, best_points)
SELECT 'overall', player_name, MAX(points) FROM scores GROUP BY player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('difficulty:', LOWER(difficulty)), player_name, MAX(points) FROM scores GROUP BY difficulty, player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('monster:', monster), player_name, MAX(points) FROM scores GROUP BY monster, player_name;

-- Leaderboards no longer aggregate over scores, so these indexes only slowed down inserts.
DROP INDEX idx_scores_difficulty ON scores;
DROP INDEX idx_scores_monster ON scores;
