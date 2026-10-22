package com.apexarena.leaderboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.dto.LeaderboardResponse;
import com.apexarena.leaderboard.service.LeaderboardService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping("/top")
    public LeaderboardResponse top(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int n,
                                   @RequestParam(required = false) String difficulty,
                                   @RequestParam(required = false) String monster) {
        return service.top(Board.from(difficulty, monster), n);
    }
}
