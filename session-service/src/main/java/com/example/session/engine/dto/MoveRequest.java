package com.example.session.engine.dto;

import com.example.session.domain.Player;

public record MoveRequest(Player player, int position) {
}
