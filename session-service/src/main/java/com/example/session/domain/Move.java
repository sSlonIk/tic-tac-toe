package com.example.session.domain;

import java.time.Instant;

public record Move(int moveNumber, Player player, int position, Instant timestamp) {
}
