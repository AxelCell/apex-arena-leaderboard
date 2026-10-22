package com.apexarena.leaderboard;

import static com.apexarena.leaderboard.TestRequests.fightJson;
import static com.apexarena.leaderboard.TestRequests.signedPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "leaderboard.cache.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
class RedisCacheContainerTest {
    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Autowired MockMvc mvc;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired JdbcTemplate jdbc;

    @Test
    void leaderboardIsServedFromRedisAndUpdatedOnWrite() throws Exception {
        mvc.perform(signedPost(fightJson("alpha", "krakoral", "hard", true, 50, 40))).andExpect(status().isCreated());

        mvc.perform(get("/api/leaderboard/top").param("monster", "krakoral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].playerName").value("alpha"));
        assertThat(redisTemplate.opsForZSet().score("lb:monster:krakoral", "alpha")).isEqualTo(449.0);

        mvc.perform(signedPost(fightJson("bravo", "krakoral", "boss", true, 90, 20))).andExpect(status().isCreated());
        assertThat(redisTemplate.opsForZSet().score("lb:monster:krakoral", "bravo")).isNotNull();

        mvc.perform(get("/api/leaderboard/top").param("monster", "krakoral"))
                .andExpect(jsonPath("$.entries[0].playerName").value("bravo"))
                .andExpect(jsonPath("$.entries[1].playerName").value("alpha"));
        mvc.perform(get("/api/players/alpha/rank").param("monster", "krakoral"))
                .andExpect(jsonPath("$.rank").value(2))
                .andExpect(jsonPath("$.totalPlayers").value(2));

        mvc.perform(signedPost(fightJson("bravo", "krakoral", "easy", false, 0, 60))).andExpect(status().isCreated());
        mvc.perform(get("/api/players/bravo/rank").param("monster", "krakoral"))
                .andExpect(jsonPath("$.rank").value(1));
    }

    @Test
    void loadsALargeBoardIntoRedis() throws Exception {
        int players = 20_000;
        jdbc.execute("SET SESSION cte_max_recursion_depth = 30000");
        jdbc.update("INSERT INTO scores (player_name, monster, difficulty, game_mode, won, hp_left_percent, "
                + "fight_seconds, points, submitted_at) "
                + "WITH RECURSIVE seq(n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < ?) "
                + "SELECT CONCAT('bulk', n), 'bulkox', 'EASY', 'QUICK', TRUE, 50, 30, MOD(n, 1250), NOW(6) FROM seq",
                players - 1);
        jdbc.update("INSERT INTO player_best (board_key, player_name, best_points) "
                + "SELECT 'monster:bulkox', player_name, MAX(points) FROM scores WHERE monster = 'bulkox' "
                + "GROUP BY player_name");

        mvc.perform(get("/api/leaderboard/top").param("monster", "bulkox").param("n", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].points").value(1249));

        assertThat(redisTemplate.opsForZSet().zCard("lb:monster:bulkox")).isEqualTo(players + 1L);
        mvc.perform(get("/api/players/bulk0/rank").param("monster", "bulkox"))
                .andExpect(jsonPath("$.totalPlayers").value(players));
    }
}
