package com.apexarena.leaderboard.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apexarena.leaderboard.domain.Difficulty;

/**
 * Spring Data generates the implementation of this interface at startup.
 * A null {@code difficulty}/{@code monster} means "no filter", so one query serves all boards.
 */
interface SpringDataScoreRepository extends JpaRepository<ScoreEntity, Long> {

    String BOARD_FILTER = "(:difficulty is null or s.difficulty = :difficulty) "
            + "and (:monster is null or s.monster = :monster)";

    @Query("select new com.apexarena.leaderboard.repository.PlayerBest(s.playerName, max(s.points)) "
            + "from ScoreEntity s where " + BOARD_FILTER
            + " group by s.playerName order by max(s.points) desc, s.playerName asc")
    List<PlayerBest> bestPerPlayer(@Param("difficulty") Difficulty difficulty,
                                   @Param("monster") String monster,
                                   Pageable page);

    @Query("select max(s.points) from ScoreEntity s where s.playerName = :player and " + BOARD_FILTER)
    Integer bestPoints(@Param("player") String player,
                       @Param("difficulty") Difficulty difficulty,
                       @Param("monster") String monster);

    // "players whose best > p" equals "players with any score > p", which avoids a subquery
    @Query("select count(distinct s.playerName) from ScoreEntity s where s.points > :points and " + BOARD_FILTER)
    long countPlayersAbove(@Param("points") int points,
                           @Param("difficulty") Difficulty difficulty,
                           @Param("monster") String monster);

    @Query("select count(distinct s.playerName) from ScoreEntity s where " + BOARD_FILTER)
    long countPlayers(@Param("difficulty") Difficulty difficulty,
                      @Param("monster") String monster);
}
