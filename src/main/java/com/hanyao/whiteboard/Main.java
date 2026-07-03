package com.hanyao.whiteboard;

import com.hanyao.whiteboard.config.AppConfig;
import com.hanyao.whiteboard.network.MessageSender;
import com.hanyao.whiteboard.network.TcpClient;
import com.hanyao.whiteboard.network.TcpServer;
import com.hanyao.whiteboard.network.UdpDiscoveryService;
import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.service.ChatService;
import com.hanyao.whiteboard.service.DrawingService;
import com.hanyao.whiteboard.service.PeerService;
import com.hanyao.whiteboard.ui.MainFrame;
import com.hanyao.whiteboard.util.ProtocolDispatcher;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    private static final String DEFAULT_USERNAME = "user-" + System.getProperty("user.name", "guest");

    public static void main(String[] args) throws IOException {
        RuntimeOptions options = RuntimeOptions.parse(args);

        PeerService peerService = new PeerService();
        DrawingService drawingService = new DrawingService();
        ChatService chatService = new ChatService();
        ProtocolDispatcher dispatcher = new ProtocolDispatcher(peerService, drawingService, chatService);
        List<AutoCloseable> closeables = new CopyOnWriteArrayList<>();

        MessageSender sender;
        if (options.serverMode()) {
            TcpServer server = new TcpServer(options.port(), dispatcher::dispatch);
            server.start();
            sender = server;
            closeables.add(server);

            UdpDiscoveryService discovery = new UdpDiscoveryService(options.username(), options.port(), peerService::upsert);
            discovery.start();
            closeables.add(discovery);
        } else {
            TcpClient client = new TcpClient(options.host(), options.port(), dispatcher::dispatch);
            client.connect();
            client.send(Message.join(options.username(), localHostAddress(), client.localPort()));
            sender = client;
            closeables.add(client);
        }

        MessageSender finalSender = sender;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            finalSender.send(Message.leave(options.username()));
            closeAll(closeables);
        }, "whiteboard-shutdown"));

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(options.username(), peerService, drawingService, chatService, finalSender);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent event) {
                    finalSender.send(Message.leave(options.username()));
                    closeAll(closeables);
                }
            });
            frame.setVisible(true);
        });
    }

    private static String localHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ex) {
            return "127.0.0.1";
        }
    }

    private static void closeAll(List<AutoCloseable> closeables) {
        List<AutoCloseable> snapshot = new ArrayList<>(closeables);
        closeables.clear();
        for (AutoCloseable closeable : snapshot) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best-effort shutdown from UI/shutdown hook
            }
        }
    }

    private record RuntimeOptions(boolean serverMode, String host, int port, String username) {
        static RuntimeOptions parse(String[] args) {
            if (args.length == 0 || "--server".equals(args[0])) {
                int port = args.length >= 2 ? Integer.parseInt(args[1]) : AppConfig.DEFAULT_TCP_PORT;
                String username = args.length >= 3 ? args[2] : DEFAULT_USERNAME;
                return new RuntimeOptions(true, "localhost", port, username);
            }

            if ("--connect".equals(args[0])) {
                String host = args.length >= 2 ? args[1] : "localhost";
                int port = args.length >= 3 ? Integer.parseInt(args[2]) : AppConfig.DEFAULT_TCP_PORT;
                String username = args.length >= 4 ? args[3] : DEFAULT_USERNAME;
                return new RuntimeOptions(false, host, port, username);
            }

            throw new IllegalArgumentException("""
                    Usage:
                      mvn exec:java -Dexec.args="--server [port] [username]"
                      mvn exec:java -Dexec.args="--connect <host> [port] [username]"
                    """);
        }
    }
}
