package com.apexarena.leaderboard;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Step 1: the simplest possible leaderboard. Everything lives in one class and
 * one in-memory list; later steps split this into controller / service / repository.
 */
@RestController
@RequestMapping("/api")
public class ScoreController {

    public record ScoreRequest(String playerName, int points) {}

    public record Score(String playerName, int points, Instant submittedAt) {}

    // Tomcat serves requests on many threads, so the list must be thread-safe.
    private final List<Score> scores = new CopyOnWriteArrayList<>();

    @PostMapping("/scores")
    @ResponseStatus(HttpStatus.CREATED)
    public Score submit(@RequestBody ScoreRequest req) {
        Score s = new Score(req.playerName(), req.points(), Instant.now());
        scores.add(s);
        return s;
    }

    @GetMapping("/leaderboard/top")
    public List<Score> top(@RequestParam(defaultValue = "10") int n) {
        return scores.stream()
                .sorted(Comparator.comparingInt(Score::points).reversed())
                .limit(n)
                .toList();
    }
}
