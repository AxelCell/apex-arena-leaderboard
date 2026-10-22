package com.apexarena.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LeaderboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaderboardApplication.class, args);
	}

}
