package com.example.engine.api.dto;

import com.example.engine.domain.Player;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
    @NotNull Player player,
    @NotNull @Min(0) @Max(8) Integer position
) {
}
