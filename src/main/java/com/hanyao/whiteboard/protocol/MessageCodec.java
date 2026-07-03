package com.hanyao.whiteboard.protocol;

import java.util.ArrayList;
import java.util.List;

public final class MessageCodec {
    private static final char DELIMITER = '|';
    private static final char ESCAPE = '\\';

    private MessageCodec() {
    }

    public static String encode(Message message) {
        List<String> parts = new ArrayList<>();
        parts.add(message.type().name());
        message.fields().stream().map(MessageCodec::escape).forEach(parts::add);
        return String.join(String.valueOf(DELIMITER), parts);
    }

    public static Message decode(String wireMessage) {
        if (wireMessage == null || wireMessage.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        List<String> parts = splitEscaped(wireMessage);
        MessageType type = MessageType.valueOf(parts.get(0));
        List<String> fields = parts.subList(1, parts.size());
        return new Message(type, fields);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char current : value.toCharArray()) {
            if (current == ESCAPE || current == DELIMITER || current == '\n' || current == '\r') {
                escaped.append(ESCAPE);
            }
            if (current == '\n') {
                escaped.append('n');
            } else if (current == '\r') {
                escaped.append('r');
            } else {
                escaped.append(current);
            }
        }
        return escaped.toString();
    }

    private static List<String> splitEscaped(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (char ch : value.toCharArray()) {
            if (escaping) {
                if (ch == 'n') {
                    current.append('\n');
                } else if (ch == 'r') {
                    current.append('\r');
                } else {
                    current.append(ch);
                }
                escaping = false;
            } else if (ch == ESCAPE) {
                escaping = true;
            } else if (ch == DELIMITER) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        if (escaping) {
            throw new IllegalArgumentException("dangling escape character");
        }

        parts.add(current.toString());
        return parts;
    }
}
