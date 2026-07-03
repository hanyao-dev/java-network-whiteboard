package com.hanyao.whiteboard.model;

import java.time.Instant;
import java.util.Objects;

public record Peer(String username, String host, int port, Instant lastSeen) {
    public Peer {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(lastSeen, "lastSeen");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public String id() {
        return username + "@" + host + ":" + port;
    }
}
