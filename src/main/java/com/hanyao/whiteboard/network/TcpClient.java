package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.config.AppConfig;
import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.protocol.MessageCodec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TcpClient implements MessageSender, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TcpClient.class.getName());

    private final String host;
    private final int port;
    private final MessageListener listener;
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Socket socket;
    private BufferedWriter writer;

    public TcpClient(String host, int port, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void connect() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), AppConfig.SOCKET_CONNECT_TIMEOUT_MILLIS);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        listenerExecutor.submit(this::listen);
    }

    public int localPort() {
        return socket == null ? -1 : socket.getLocalPort();
    }

    private void listen() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                try {
                    listener.onMessage(MessageCodec.decode(line));
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.WARNING, "Ignoring invalid server message: " + line, ex);
                }
            }
        } catch (IOException ex) {
            if (running.get()) {
                LOGGER.log(Level.INFO, "Disconnected from server", ex);
            }
        } finally {
            close();
        }
    }

    @Override
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
                    LOGGER.log(Level.INFO, "Failed to send message to server", ex);
                    close();
                }
            }
        });
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Socket already closed", ex);
        }
        shutdownExecutor(listenerExecutor);
        shutdownExecutor(sendExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdownNow();
        try {
            executor.awaitTermination(AppConfig.SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
