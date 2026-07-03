package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.protocol.Message;

@FunctionalInterface
public interface MessageSender {
    void send(Message message);
}
