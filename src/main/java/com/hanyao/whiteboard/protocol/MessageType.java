package com.hanyao.whiteboard.protocol;

public enum MessageType {
    JOIN(3),
    LEAVE(1),
    DRAW(6),
    CHAT(2),
    CLEAR(1),
    PING(1),
    PONG(1);

    private final int fieldCount;

    MessageType(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    public int fieldCount() {
        return fieldCount;
    }
}
