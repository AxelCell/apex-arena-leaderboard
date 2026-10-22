package com.apexarena.leaderboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.CREATED)
    public ScoreResponse submit(@Valid @RequestBody SubmitScoreRequest req) {
        return service.submit(req);
    }
}
