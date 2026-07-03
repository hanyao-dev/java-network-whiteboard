package com.hanyao.whiteboard.model;

import java.time.Instant;
import java.util.Objects;

public record ChatMessage(String username, String message, Instant timestamp) {
    public ChatMessage {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(timestamp, "timestamp");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
