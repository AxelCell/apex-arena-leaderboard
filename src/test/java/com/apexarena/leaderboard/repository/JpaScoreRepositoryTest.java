package com.apexarena.leaderboard.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.GameMode;
import com.apexarena.leaderboard.domain.Score;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:repotest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaScoreRepository.class)
class JpaScoreRepositoryTest {
    @Autowired JpaScoreRepository repo;

    private static Score score(String player, String monster, Difficulty d, int points) {
        return new Score(null, player, monster, d, GameMode.QUICK, true, 50, 30, points, Instant.now(),
                UUID.randomUUID().toString());
    }

    @BeforeEach
    void seed() {
        repo.save(score("aish", "zapret", Difficulty.BOSS, 900));
        repo.save(score("aish", "golmoss", Difficulty.EASY, 200));
        repo.save(score("bob", "zapret", Difficulty.HARD, 500));
        repo.save(score("bob", "zapret", Difficulty.HARD, 650));
        repo.save(score("cat", "golmoss", Difficulty.EASY, 650));
    }

    @Test
    void topPlayersUsesEachPlayersBestOnce() {
        assertThat(repo.topPlayers(Board.overall(), 10)).containsExactly(
                new PlayerBest("aish", 900), new PlayerBest("bob", 650), new PlayerBest("cat", 650));
    }

    @Test
    void limitIsApplied() {
        assertThat(repo.topPlayers(Board.overall(), 2)).hasSize(2);
    }

    @Test
    void boardsFilterByDifficultyAndMonster() {
        assertThat(repo.topPlayers(Board.of(Difficulty.EASY), 10))
                .containsExactly(new PlayerBest("cat", 650), new PlayerBest("aish", 200));
        assertThat(repo.topPlayers(Board.ofMonster("zapret"), 10))
                .containsExactly(new PlayerBest("aish", 900), new PlayerBest("bob", 650));
    }

    @Test
    void bestPointsAndRankCounts() {
        assertThat(repo.bestPoints(Board.overall(), "bob")).contains(650);
        assertThat(repo.bestPoints(Board.of(Difficulty.BOSS), "bob")).isEmpty();
        assertThat(repo.countPlayersAbove(Board.overall(), 650)).isEqualTo(1);
        assertThat(repo.countPlayersAbove(Board.overall(), 100)).isEqualTo(3);
        assertThat(repo.countPlayers(Board.overall())).isEqualTo(3);
        assertThat(repo.countPlayers(Board.ofMonster("golmoss"))).isEqualTo(2);
    }

    @Test
    void fightIdIsUnique() {
        Score s = score("dan", "zapret", Difficulty.EASY, 100);
        repo.save(s);
        assertThat(repo.existsByFightId(s.fightId())).isTrue();
        assertThatThrownBy(() -> repo.save(s)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
