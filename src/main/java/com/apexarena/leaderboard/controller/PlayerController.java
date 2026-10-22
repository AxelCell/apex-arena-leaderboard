package com.apexarena.leaderboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.dto.PlayerRankResponse;
import com.apexarena.leaderboard.service.LeaderboardService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final LeaderboardService service;

    public PlayerController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping("/{name}/rank")
    public PlayerRankResponse rank(@PathVariable String name,
                                   @RequestParam(required = false) String difficulty,
                                   @RequestParam(required = false) String monster) {
        return service.rank(name, Board.from(difficulty, monster));
    }
}
