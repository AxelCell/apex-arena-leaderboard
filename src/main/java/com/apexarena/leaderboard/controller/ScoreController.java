package com.apexarena.leaderboard.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.dto.ScoreResponse;
import com.apexarena.leaderboard.dto.SubmitScoreRequest;
import com.apexarena.leaderboard.service.LeaderboardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final LeaderboardService service;

    public ScoreController(LeaderboardService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ScoreResponse> submit(@Valid @RequestBody SubmitScoreRequest req) {
        Score saved = service.submit(req);
        return ResponseEntity.created(URI.create("/api/scores/" + saved.id()))
                .body(ScoreResponse.from(saved));
    }
}
