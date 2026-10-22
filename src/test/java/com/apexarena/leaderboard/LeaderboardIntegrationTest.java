package com.apexarena.leaderboard;

import static com.apexarena.leaderboard.TestRequests.fightJson;
import static com.apexarena.leaderboard.TestRequests.signedPost;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaderboardIntegrationTest {
    @Autowired MockMvc mvc;

    private static String uniqueName() {
        return "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void submitThenReadLeaderboardAndRank() throws Exception {
        String a = uniqueName();
        String b = uniqueName();
        mvc.perform(signedPost(fightJson(a, "oraclyx", "boss", true, 60, 45)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.points").value(936));
        mvc.perform(signedPost(fightJson(b, "oraclyx", "boss", true, 10, 90)))
                .andExpect(status().isCreated());
        mvc.perform(signedPost(fightJson(a, "oraclyx", "boss", false, 0, 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.points").value(50))
                .andExpect(jsonPath("$.bestPoints").value(936));

        mvc.perform(get("/api/players/" + a + "/rank").param("monster", "oraclyx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(1))
                .andExpect(jsonPath("$.bestPoints").value(936));
        mvc.perform(get("/api/players/" + b + "/rank").param("monster", "oraclyx"))
                .andExpect(jsonPath("$.rank").value(2));
    }

    @Test
    void replayedSubmissionIsRejected() throws Exception {
        String json = fightJson(uniqueName(), "zapret", "easy", true, 50, 30);
        mvc.perform(signedPost(json)).andExpect(status().isCreated());
        mvc.perform(signedPost(json)).andExpect(status().isConflict());
    }

    @Test
    void impossibleResultIsRejected() throws Exception {
        mvc.perform(signedPost(fightJson(uniqueName(), "zapret", "easy", true, 0, 30)))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(signedPost(fightJson(uniqueName(), "godzilla", "easy", true, 50, 30)))
                .andExpect(status().isUnprocessableContent());
    }
}
