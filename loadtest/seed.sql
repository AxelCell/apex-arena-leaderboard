SET SESSION cte_max_recursion_depth = 250000;

INSERT INTO scores (player_name, monster, difficulty, game_mode, won, hp_left_percent, fight_seconds, points,
                    submitted_at)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200000
)
SELECT CONCAT('player', LPAD(1 + MOD(n * 7919, 20000), 5, '0')),
       ELT(1 + MOD(n, 16), 'cindercub', 'tidepup', 'sproutle', 'zapret', 'fulgurr', 'vespike', 'gloomwisp',
           'ironhorn', 'golmoss', 'bulkox', 'zephyrax', 'sandrex', 'blazewing', 'krakoral', 'mossgore', 'oraclyx'),
       ELT(1 + MOD(n DIV 16, 3), 'EASY', 'HARD', 'BOSS'),
       IF(MOD(n, 4) = 0, 'LEAGUE', 'QUICK'),
       TRUE,
       1 + FLOOR(RAND(n) * 100),
       5 + FLOOR(RAND(n + 1) * 90),
       FLOOR(RAND(n + 2) * 1250),
       NOW(6)
FROM seq;

TRUNCATE TABLE player_best;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT 'overall', player_name, MAX(points) FROM scores GROUP BY player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('difficulty:', LOWER(difficulty)), player_name, MAX(points) FROM scores GROUP BY difficulty, player_name;
INSERT INTO player_best (board_key, player_name, best_points)
SELECT CONCAT('monster:', monster), player_name, MAX(points) FROM scores GROUP BY monster, player_name;

ANALYZE TABLE scores, player_best;
