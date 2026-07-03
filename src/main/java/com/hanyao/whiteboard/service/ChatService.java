package com.hanyao.whiteboard.service;

import com.hanyao.whiteboard.model.ChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ChatService {
    private final CopyOnWriteArrayList<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ChatMessage>> listeners = new CopyOnWriteArrayList<>();

    public void addMessage(String username, String message) {
        ChatMessage chatMessage = new ChatMessage(username, message, Instant.now());
        messages.add(chatMessage);
        listeners.forEach(listener -> listener.accept(chatMessage));
    }

    public List<ChatMessage> history() {
        return List.copyOf(messages);
    }

    public void addListener(Consumer<ChatMessage> listener) {
        listeners.add(listener);
    }
}
