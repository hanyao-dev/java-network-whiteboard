package com.hanyao.whiteboard.util;

import com.hanyao.whiteboard.model.DrawCommand;
import com.hanyao.whiteboard.model.Peer;
import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.protocol.MessageType;
import com.hanyao.whiteboard.service.ChatService;
import com.hanyao.whiteboard.service.DrawingService;
import com.hanyao.whiteboard.service.PeerService;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProtocolDispatcher {
    private static final Logger LOGGER = Logger.getLogger(ProtocolDispatcher.class.getName());

    private final PeerService peerService;
    private final DrawingService drawingService;
    private final ChatService chatService;

    public ProtocolDispatcher(PeerService peerService, DrawingService drawingService, ChatService chatService) {
        this.peerService = peerService;
        this.drawingService = drawingService;
        this.chatService = chatService;
    }

    public void dispatch(Message message) {
        try {
            if (message.type() == MessageType.JOIN) {
                peerService.upsert(new Peer(message.fields().get(0), message.fields().get(1),
                        Integer.parseInt(message.fields().get(2)), Instant.now()));
            } else if (message.type() == MessageType.LEAVE) {
                peerService.remove(message.fields().get(0));
            } else if (message.type() == MessageType.DRAW) {
                drawingService.addDraw(DrawCommand.fromWire(
                        Integer.parseInt(message.fields().get(0)),
                        Integer.parseInt(message.fields().get(1)),
                        Integer.parseInt(message.fields().get(2)),
                        Integer.parseInt(message.fields().get(3)),
                        message.fields().get(4),
                        Integer.parseInt(message.fields().get(5))));
            } else if (message.type() == MessageType.CHAT) {
                chatService.addMessage(message.fields().get(0), message.fields().get(1));
            } else if (message.type() == MessageType.CLEAR) {
                drawingService.clear();
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Failed to dispatch protocol message: " + message, ex);
        }
    }
}
