package com.hanyao.whiteboard.protocol;

import java.util.List;
import java.util.Objects;

public record Message(MessageType type, List<String> fields) {
    public Message {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fields, "fields");
        fields = List.copyOf(fields);
        if (fields.size() != type.fieldCount()) {
            throw new IllegalArgumentException(
                    "%s requires %d fields but got %d".formatted(type, type.fieldCount(), fields.size()));
        }
    }

    public static Message join(String username, String ip, int port) {
        return new Message(MessageType.JOIN, List.of(username, ip, Integer.toString(port)));
    }

    public static Message leave(String username) {
        return new Message(MessageType.LEAVE, List.of(username));
    }

    public static Message draw(String x1, String y1, String x2, String y2, String color, String strokeWidth) {
        return new Message(MessageType.DRAW, List.of(x1, y1, x2, y2, color, strokeWidth));
    }

    public static Message chat(String username, String message) {
        return new Message(MessageType.CHAT, List.of(username, message));
    }

    public static Message clear(String username) {
        return new Message(MessageType.CLEAR, List.of(username));
    }

    public static Message ping(String username) {
        return new Message(MessageType.PING, List.of(username));
    }

    public static Message pong(String username) {
        return new Message(MessageType.PONG, List.of(username));
    }
}
