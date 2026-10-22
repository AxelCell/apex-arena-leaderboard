package com.apexarena.leaderboard.controller;

import static com.apexarena.leaderboard.TestRequests.fightJson;
import static com.apexarena.leaderboard.TestRequests.signedPost;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.apexarena.leaderboard.ClockConfig;
import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.dto.LeaderboardEntry;
import com.apexarena.leaderboard.dto.LeaderboardResponse;
import com.apexarena.leaderboard.dto.ScoreResponse;
import com.apexarena.leaderboard.exception.PlayerNotFoundException;
import com.apexarena.leaderboard.service.LeaderboardService;

/**
 * Web layer only: controllers, validation, exception handler and the security filters.
 * The service is a Mockito mock, so no database is involved.
 */
@WebMvcTest(controllers = { ScoreController.class, LeaderboardController.class, PlayerController.class })
@Import(ClockConfig.class)
@ActiveProfiles("test")
class ScoreControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean LeaderboardService service;

    @Test
    void validSignedSubmissionReturns201WithLocation() throws Exception {
        when(service.submit(any())).thenReturn(new ScoreResponse(7L, "aish", "zapret", Difficulty.HARD, true,
                537, 537, 1, Instant.now()));

        mvc.perform(signedPost(fightJson("aish", "zapret", "hard", true, 80, 30)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/scores/7"))
                .andExpect(jsonPath("$.points").value(537))
                .andExpect(jsonPath("$.difficulty").value("hard"));
    }

    @Test
    void invalidFieldsReturn400WithFieldErrors() throws Exception {
        String json = """
                {"playerName":"x!","monster":"","difficulty":"hard","mode":"quick","won":true,
                "hpLeftPercent":150,"fightSeconds":30,"fightId":"abc"}""";
        mvc.perform(signedPost(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.playerName").exists())
                .andExpect(jsonPath("$.errors.monster").exists())
                .andExpect(jsonPath("$.errors.hpLeftPercent").exists());
        verify(service, never()).submit(any());
    }

    @Test
    void unknownDifficultyReturns400() throws Exception {
        mvc.perform(signedPost(fightJson("aish", "zapret", "nightmare", true, 80, 30)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsignedSubmissionIsRejectedBeforeTheController() throws Exception {
        mvc.perform(post("/api/scores").contentType(MediaType.APPLICATION_JSON)
                        .content(fightJson("aish", "zapret", "hard", true, 80, 30)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Missing X-Timestamp or X-Signature header"));
        verify(service, never()).submit(any());
    }

    @Test
    void leaderboardQueryParamsSelectTheBoard() throws Exception {
        when(service.top(Board.ofMonster("zapret"), 5)).thenReturn(new LeaderboardResponse("monster:zapret",
                List.of(new LeaderboardEntry(1, "aish", 936))));

        mvc.perform(get("/api/leaderboard/top").param("n", "5").param("monster", "zapret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").value("monster:zapret"))
                .andExpect(jsonPath("$.entries[0].playerName").value("aish"));
    }

    @Test
    void badLeaderboardParamsReturn400() throws Exception {
        mvc.perform(get("/api/leaderboard/top").param("n", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaderboard/top").param("n", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/leaderboard/top").param("difficulty", "hard").param("monster", "zapret"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPlayerReturns404() throws Exception {
        when(service.rank("ghost", Board.overall())).thenThrow(new PlayerNotFoundException("ghost", "overall"));
        mvc.perform(get("/api/players/ghost/rank"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Player not found"));
    }
}
