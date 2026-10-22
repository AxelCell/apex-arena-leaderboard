package com.apexarena.leaderboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Request body for POST /api/scores. Validated before it reaches the service. */
public record SubmitScoreRequest(
        @NotNull
        @Pattern(regexp = "^[A-Za-z0-9_]{3,16}$",
                message = "must be 3-16 characters: letters, digits or underscore")
        String playerName,

        @NotNull @Min(0) @Max(100_000)
        Integer points
) {}
