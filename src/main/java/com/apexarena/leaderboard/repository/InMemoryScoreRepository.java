package com.apexarena.leaderboard.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Score;

@Repository
public class InMemoryScoreRepository implements ScoreRepository {

    private final List<Score> scores = new CopyOnWriteArrayList<>();
    // board -> (player -> best points). merge(..., Math::max) is atomic per key.
    private final Map<Board, Map<String, Integer>> best = new ConcurrentHashMap<>();

    @Override
    public Score save(Score score) {
        scores.add(score);
        for (Board b : Board.boardsFor(score)) {
            best.computeIfAbsent(b, k -> new ConcurrentHashMap<>())
                    .merge(score.playerName(), score.points(), Math::max);
        }
        return score;
    }

    @Override
    public List<PlayerBest> topPlayers(Board board, int limit) {
        return board(board).entrySet().stream()
                .map(e -> new PlayerBest(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(PlayerBest::points).reversed()
                        .thenComparing(PlayerBest::playerName))
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<Integer> bestPoints(Board board, String playerName) {
        return Optional.ofNullable(board(board).get(playerName));
    }

    @Override
    public long countPlayersAbove(Board board, int points) {
        return board(board).values().stream().filter(p -> p > points).count();
    }

    @Override
    public long countPlayers(Board board) {
        return board(board).size();
    }

    private Map<String, Integer> board(Board board) {
        return best.getOrDefault(board, Map.of());
    }
}
