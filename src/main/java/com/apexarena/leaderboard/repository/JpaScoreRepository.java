package com.apexarena.leaderboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.Score;

/** Adapts the Spring Data repository to our {@link ScoreRepository} interface. */
@Repository
public class JpaScoreRepository implements ScoreRepository {

    private final SpringDataScoreRepository jpa;

    public JpaScoreRepository(SpringDataScoreRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Score save(Score score) {
        // saveAndFlush: run the INSERT now, so a duplicate fightId fails inside this call
        return jpa.saveAndFlush(ScoreEntity.from(score)).toDomain();
    }

    @Override
    public boolean existsByFightId(String fightId) {
        return jpa.existsByFightId(fightId);
    }

    @Override
    public List<PlayerBest> topPlayers(Board board, int limit) {
        return jpa.bestPerPlayer(difficulty(board), monster(board), PageRequest.of(0, limit));
    }

    @Override
    public List<PlayerBest> allPlayers(Board board) {
        return jpa.bestPerPlayer(difficulty(board), monster(board), Pageable.unpaged());
    }

    @Override
    public Optional<Integer> bestPoints(Board board, String playerName) {
        return Optional.ofNullable(jpa.bestPoints(playerName, difficulty(board), monster(board)));
    }

    @Override
    public long countPlayersAbove(Board board, int points) {
        return jpa.countPlayersAbove(points, difficulty(board), monster(board));
    }

    @Override
    public long countPlayers(Board board) {
        return jpa.countPlayers(difficulty(board), monster(board));
    }

    private static Difficulty difficulty(Board b) {
        return b.type() == Board.Type.DIFFICULTY ? Difficulty.from(b.value()) : null;
    }

    private static String monster(Board b) {
        return b.type() == Board.Type.MONSTER ? b.value() : null;
    }
}
