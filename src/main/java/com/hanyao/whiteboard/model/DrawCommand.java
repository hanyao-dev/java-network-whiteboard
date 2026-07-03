package com.hanyao.whiteboard.model;

import java.awt.Color;

public record DrawCommand(int x1, int y1, int x2, int y2, Color color, int strokeWidth) {
    public DrawCommand {
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        if (strokeWidth <= 0) {
            throw new IllegalArgumentException("strokeWidth must be positive");
        }
    }

    public String colorAsHex() {
        return "#%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static DrawCommand fromWire(int x1, int y1, int x2, int y2, String colorHex, int strokeWidth) {
        return new DrawCommand(x1, y1, x2, y2, Color.decode(colorHex), strokeWidth);
    }
}
