package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.protocol.MessageCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClientHandler implements Runnable, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final BiConsumer<ClientHandler, Message> messageConsumer;
    private final ExecutorService sendExecutor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final BufferedWriter writer;

    public ClientHandler(Socket socket, BiConsumer<ClientHandler, Message> messageConsumer, ExecutorService sendExecutor)
            throws IOException {
        this.socket = socket;
        this.messageConsumer = messageConsumer;
        this.sendExecutor = sendExecutor;
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                try {
                    messageConsumer.accept(this, MessageCodec.decode(line));
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.WARNING, "Ignoring invalid client message: " + line, ex);
                }
            }
        } catch (IOException ex) {
            if (running.get()) {
                LOGGER.log(Level.INFO, "Client connection closed unexpectedly", ex);
            }
        } finally {
            close();
        }
    }

    public void send(Message message) {
        if (!running.get()) {
            return;
        }
        sendExecutor.submit(() -> {
            synchronized (writer) {
                try {
                    writer.write(MessageCodec.encode(message));
                    writer.newLine();
                    writer.flush();
                } catch (IOException ex) {
                    LOGGER.log(Level.INFO, "Failed to send message to client", ex);
                    close();
                }
            }
        });
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                socket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Socket already closed", ex);
            }
        }
    }
}
