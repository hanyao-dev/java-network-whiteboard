package com.hanyao.whiteboard.service;

import com.hanyao.whiteboard.model.DrawCommand;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class DrawingService {
    private final CopyOnWriteArrayList<DrawCommand> history = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<DrawCommand>> drawListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> clearListeners = new CopyOnWriteArrayList<>();

    public void addDraw(DrawCommand command) {
        history.add(command);
        drawListeners.forEach(listener -> listener.accept(command));
    }

    public void clear() {
        history.clear();
        clearListeners.forEach(Runnable::run);
    }

    public List<DrawCommand> history() {
        return List.copyOf(history);
    }

    public void addDrawListener(Consumer<DrawCommand> listener) {
        drawListeners.add(listener);
    }

    public void addClearListener(Runnable listener) {
        clearListeners.add(listener);
    }
}
