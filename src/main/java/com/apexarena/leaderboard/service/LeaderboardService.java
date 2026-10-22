package com.apexarena.leaderboard.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.dto.LeaderboardEntry;
import com.apexarena.leaderboard.dto.SubmitScoreRequest;
import com.apexarena.leaderboard.repository.ScoreRepository;

@Service
public class LeaderboardService {

    private final ScoreRepository repository;
    private final Clock clock;

    // Constructor injection: Spring passes in the beans; fields stay final and testable.
    public LeaderboardService(ScoreRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Score submit(SubmitScoreRequest req) {
        Score score = new Score(UUID.randomUUID(), req.playerName(), req.points(), Instant.now(clock));
        return repository.save(score);
    }

    public List<LeaderboardEntry> top(int n) {
        List<ScoreRepository.PlayerBest> rows = repository.topPlayers(n);
        List<LeaderboardEntry> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            out.add(new LeaderboardEntry(i + 1, rows.get(i).playerName(), rows.get(i).points()));
        }
        return out;
    }
}
