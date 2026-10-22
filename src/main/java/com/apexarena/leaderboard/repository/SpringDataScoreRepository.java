package com.apexarena.leaderboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataScoreRepository extends JpaRepository<ScoreEntity, Long> {
    boolean existsByFightId(String fightId);
}
