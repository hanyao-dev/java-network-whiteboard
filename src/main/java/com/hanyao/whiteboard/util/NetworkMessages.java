package com.hanyao.whiteboard.util;

import com.hanyao.whiteboard.model.DrawCommand;
import com.hanyao.whiteboard.protocol.Message;

public final class NetworkMessages {
    private NetworkMessages() {
    }

    public static Message draw(DrawCommand command) {
        return Message.draw(
                Integer.toString(command.x1()),
                Integer.toString(command.y1()),
                Integer.toString(command.x2()),
                Integer.toString(command.y2()),
                command.colorAsHex(),
                Integer.toString(command.strokeWidth()));
    }
}
