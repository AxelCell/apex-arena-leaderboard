package com.apexarena.leaderboard.dto;

import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.GameMode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitScoreRequest(
        @NotNull
        @Pattern(regexp = "^[A-Za-z0-9_]{3,16}$",
                message = "must be 3-16 characters: letters, digits or underscore")
        String playerName,

        @NotBlank
        String monster,

        @NotNull
        Difficulty difficulty,

        @NotNull
        GameMode mode,

        @NotNull
        Boolean won,

        @NotNull @Min(0) @Max(100)
        Integer hpLeftPercent,

        @NotNull @DecimalMin("0.0") @DecimalMax("99.0")
        Double fightSeconds,

        @NotBlank @Size(max = 36)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "must be a UUID")
        String fightId
) {}
