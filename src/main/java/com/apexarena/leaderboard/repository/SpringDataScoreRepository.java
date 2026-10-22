package com.apexarena.leaderboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation of this interface at startup. */
interface SpringDataScoreRepository extends JpaRepository<ScoreEntity, Long> {

    // Derived query: Spring Data builds "select count(*) > 0 ... where fight_id = ?" from the name.
    boolean existsByFightId(String fightId);
}
