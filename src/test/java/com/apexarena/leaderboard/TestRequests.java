package com.apexarena.leaderboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.apexarena.leaderboard.security.HmacSigner;

public final class TestRequests {
    public static final String SECRET = "test-secret";
    private static final HmacSigner SIGNER = new HmacSigner(SECRET);

    private TestRequests() {
    }

    public static String fightJson(String player, String monster, String difficulty, boolean won, int hp,
                                   double seconds) {
        return """
                {"playerName":"%s","monster":"%s","difficulty":"%s","mode":"quick","won":%s,\
                "hpLeftPercent":%d,"fightSeconds":%s,"fightId":"%s"}"""
                .formatted(player, monster, difficulty, won, hp, seconds, UUID.randomUUID());
    }

    public static MockHttpServletRequestBuilder signedPost(String json) {
        String ts = Long.toString(Instant.now().getEpochSecond());
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        return post("/api/scores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Timestamp", ts)
                .header("X-Signature", SIGNER.sign(ts, body));
    }
}
