package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.protocol.Message;

@FunctionalInterface
public interface MessageListener {
    void onMessage(Message message);
}
