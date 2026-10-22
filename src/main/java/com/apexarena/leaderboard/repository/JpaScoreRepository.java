package com.apexarena.leaderboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;

@Repository
public class JpaScoreRepository implements ScoreRepository {
    private static final String UPSERT_BEST = """
            INSERT INTO player_best (board_key, player_name, best_points) VALUES (:board, :player, :points)
            ON DUPLICATE KEY UPDATE best_points = GREATEST(best_points, VALUES(best_points))""";

    private static final RowMapper<PlayerBest> PLAYER_BEST =
            (rs, i) -> new PlayerBest(rs.getString("player_name"), rs.getInt("best_points"));

    private final SpringDataScoreRepository jpa;
    private final JdbcClient jdbc;

    public JpaScoreRepository(SpringDataScoreRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Score save(Score score) {
        Score saved = jpa.saveAndFlush(ScoreEntity.from(score)).toDomain();
        for (Board b : Board.boardsFor(saved)) {
            jdbc.sql(UPSERT_BEST)
                    .param("board", b.key())
                    .param("player", saved.playerName())
                    .param("points", saved.points())
                    .update();
        }
        return saved;
    }

    @Override
    public boolean existsByFightId(String fightId) {
        return jpa.existsByFightId(fightId);
    }

    @Override
    public List<PlayerBest> topPlayers(Board board, int limit) {
        return jdbc.sql("""
                        SELECT player_name, best_points FROM player_best WHERE board_key = :board
                        ORDER BY best_points DESC, player_name LIMIT :limit""")
                .param("board", board.key())
                .param("limit", limit)
                .query(PLAYER_BEST)
                .list();
    }

    @Override
    public List<PlayerBest> allPlayers(Board board) {
        return jdbc.sql("SELECT player_name, best_points FROM player_best WHERE board_key = :board")
                .param("board", board.key())
                .query(PLAYER_BEST)
                .list();
    }

    @Override
    public Optional<Integer> bestPoints(Board board, String playerName) {
        return jdbc.sql("SELECT best_points FROM player_best WHERE board_key = :board AND player_name = :player")
                .param("board", board.key())
                .param("player", playerName)
                .query(Integer.class)
                .optional();
    }

    @Override
    public long countPlayersAbove(Board board, int points) {
        return jdbc.sql("SELECT COUNT(*) FROM player_best WHERE board_key = :board AND best_points > :points")
                .param("board", board.key())
                .param("points", points)
                .query(Long.class)
                .single();
    }

    @Override
    public long countPlayers(Board board) {
        return jdbc.sql("SELECT COUNT(*) FROM player_best WHERE board_key = :board")
                .param("board", board.key())
                .query(Long.class)
                .single();
    }
}
