package com.apexarena.leaderboard.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.apexarena.leaderboard.config.GameProperties;
import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.dto.LeaderboardEntry;
import com.apexarena.leaderboard.dto.LeaderboardResponse;
import com.apexarena.leaderboard.dto.PlayerRankResponse;
import com.apexarena.leaderboard.dto.ScoreResponse;
import com.apexarena.leaderboard.dto.SubmitScoreRequest;
import com.apexarena.leaderboard.exception.InvalidScoreException;
import com.apexarena.leaderboard.exception.PlayerNotFoundException;
import com.apexarena.leaderboard.repository.ScoreRepository;
import com.apexarena.leaderboard.repository.ScoreRepository.PlayerBest;

@Service
public class LeaderboardService {

    private final ScoreRepository repository;
    private final ScoreCalculator calculator;
    private final Set<String> monsters;
    private final Clock clock;

    // Constructor injection: Spring passes in the beans; fields stay final and testable.
    public LeaderboardService(ScoreRepository repository, ScoreCalculator calculator,
                              GameProperties props, Clock clock) {
        this.repository = repository;
        this.calculator = calculator;
        this.monsters = props.monsterSet();
        this.clock = clock;
    }

    public ScoreResponse submit(SubmitScoreRequest req) {
        String monster = req.monster().trim().toLowerCase(Locale.ROOT);
        if (!monsters.contains(monster)) {
            throw new InvalidScoreException("Unknown monster '" + req.monster() + "'");
        }
        int points = calculator.points(req.difficulty(), req.won(), req.hpLeftPercent(), req.fightSeconds());
        Score score = new Score(UUID.randomUUID(), req.playerName(), monster, req.difficulty(), req.mode(),
                req.won(), req.hpLeftPercent(), req.fightSeconds(), points, Instant.now(clock));
        repository.save(score);

        PlayerRankResponse rank = rank(req.playerName(), Board.overall());
        return ScoreResponse.from(score, rank.bestPoints(), rank.rank());
    }

    public LeaderboardResponse top(Board board, int n) {
        List<PlayerBest> rows = repository.topPlayers(board, n);
        List<LeaderboardEntry> entries = new ArrayList<>(rows.size());
        int rank = 0;
        for (int i = 0; i < rows.size(); i++) {
            // Standard competition ranking: equal points share a rank (1, 2, 2, 4).
            if (i == 0 || rows.get(i).points() != rows.get(i - 1).points()) {
                rank = i + 1;
            }
            entries.add(new LeaderboardEntry(rank, rows.get(i).playerName(), rows.get(i).points()));
        }
        return new LeaderboardResponse(board.key(), entries);
    }

    public PlayerRankResponse rank(String playerName, Board board) {
        int best = repository.bestPoints(board, playerName)
                .orElseThrow(() -> new PlayerNotFoundException(playerName, board.key()));
        long above = repository.countPlayersAbove(board, best);
        return new PlayerRankResponse(playerName, board.key(), (int) above + 1, best, repository.countPlayers(board));
    }
}
