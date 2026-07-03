package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.config.AppConfig;
import com.hanyao.whiteboard.protocol.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TcpServer implements MessageSender, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

    private final int port;
    private final MessageListener listener;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private final ExecutorService sendExecutor = Executors.newFixedThreadPool(4);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public TcpServer(int port, MessageListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        serverSocket = new ServerSocket(port);
        acceptExecutor.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this::handleClientMessage, sendExecutor);
                clients.add(handler);
                clientExecutor.submit(handler);
            } catch (SocketException ex) {
                if (running.get()) {
                    LOGGER.log(Level.WARNING, "TCP accept loop stopped by socket error", ex);
                }
            } catch (IOException ex) {
                if (running.get()) {
                    LOGGER.log(Level.WARNING, "Failed to accept TCP client", ex);
                }
            }
        }
    }

    private void handleClientMessage(ClientHandler source, Message message) {
        listener.onMessage(message);
        broadcastExcept(message, source);
    }

    @Override
    public void send(Message message) {
        broadcastExcept(message, null);
    }

    private void broadcastExcept(Message message, ClientHandler excludedClient) {
        for (ClientHandler client : clients) {
            if (client != excludedClient) {
                client.send(message);
            }
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        clients.forEach(ClientHandler::close);
        closeServerSocket();
        shutdownExecutor(acceptExecutor);
        shutdownExecutor(clientExecutor);
        shutdownExecutor(sendExecutor);
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Server socket already closed", ex);
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(AppConfig.SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.warning("Executor did not terminate cleanly");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
