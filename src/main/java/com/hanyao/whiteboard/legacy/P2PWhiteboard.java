package com.hanyao.whiteboard.legacy;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

public class P2PWhiteboard {
    private static final int TCP_PORT = 50001;
    private static final int DISCOVERY_PORT = 50000;
    private static final String DISCOVERY_ADDRESS = "239.255.42.99";
    private static final String HELLO_PREFIX = "P2P_WHITEBOARD_HELLO:";

    private final WhiteboardPanel whiteboardPanel;
    private final MainFrame mainFrame;
    private final String username;
    private final String nodeId;
    private final Set<String> peers = ConcurrentHashMap.newKeySet();
    private final IntConsumer peerCountListener;

    public P2PWhiteboard(WhiteboardPanel whiteboardPanel, MainFrame mainFrame, String username,
            IntConsumer peerCountListener) {
        this.whiteboardPanel = whiteboardPanel;
        this.mainFrame = mainFrame;
        this.username = username;
        this.peerCountListener = peerCountListener;
        this.nodeId = username + "-" + System.currentTimeMillis();
    }

    public void start(String manualPeerIp) {
        startServer();
        startDiscoveryListener();
        startAnnouncementLoop();
        if (manualPeerIp != null && !manualPeerIp.trim().isEmpty()) {
            addPeer(manualPeerIp.trim());
            sendHello(manualPeerIp.trim());
        }
    }

    private void startServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleConnection(socket), "p2p-client-handler").start();
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> mainFrame.showNetworkError("TCP server failed: " + e.getMessage()));
            }
        }, "p2p-tcp-server");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void handleConnection(Socket socket) {
        try (Socket closeable = socket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(closeable.getInputStream(), StandardCharsets.UTF_8))) {
            addPeer(closeable.getInetAddress().getHostAddress());
            String msg;
            while ((msg = in.readLine()) != null) {
                handleMessage(msg);
            }
        } catch (IOException ignored) {
            // A peer can close the connection at any time.
        }
    }

    private void handleMessage(String msg) {
        try {
            String[] parts = msg.split("\\|", -1);
            switch (parts[0]) {
                case "HELLO":
                    break;
                case "DRAW":
                    int x1 = Integer.parseInt(parts[1]);
                    int y1 = Integer.parseInt(parts[2]);
                    int x2 = Integer.parseInt(parts[3]);
                    int y2 = Integer.parseInt(parts[4]);
                    Color color = new Color(
                            Integer.parseInt(parts[5]),
                            Integer.parseInt(parts[6]),
                            Integer.parseInt(parts[7]));
                    int size = Integer.parseInt(parts[8]);
                    SwingUtilities.invokeLater(() -> whiteboardPanel.drawLine(x1, y1, x2, y2, color, size));
                    break;
                case "CLEAR":
                    SwingUtilities.invokeLater(() -> whiteboardPanel.clear());
                    break;
                case "CHAT":
                    String sender = decode(parts[1]);
                    String text = decode(parts[2]);
                    SwingUtilities.invokeLater(() -> mainFrame.getChatPanel().appendMessage(sender + ": " + text));
                    break;
                case "TEXTS":
                    String snapshot = decode(parts[1]);
                    SwingUtilities.invokeLater(() -> whiteboardPanel.applyTextSnapshot(snapshot));
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("Invalid peer message: " + msg);
        }
    }

    private void startDiscoveryListener() {
        Thread listenerThread = new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(DISCOVERY_PORT)) {
                InetAddress group = InetAddress.getByName(DISCOVERY_ADDRESS);
                NetworkInterface netIf = findMulticastInterface();
                if (netIf == null) {
                    throw new IOException("No multicast-capable network interface found");
                }
                socket.joinGroup(new InetSocketAddress(group, DISCOVERY_PORT), netIf);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (message.startsWith(HELLO_PREFIX) && !message.endsWith(nodeId)) {
                        String peerIp = packet.getAddress().getHostAddress();
                        addPeer(peerIp);
                        sendHello(peerIp);
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> mainFrame.showNetworkWarning("UDP discovery failed: " + e.getMessage()));
            }
        }, "p2p-discovery-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private NetworkInterface findMulticastInterface() throws SocketException {
        for (NetworkInterface netIf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (netIf.isUp() && netIf.supportsMulticast() && !netIf.isLoopback()) {
                return netIf;
            }
        }
        return null;
    }

    private void startAnnouncementLoop() {
        Thread announceThread = new Thread(() -> {
            while (true) {
                announcePresence();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "p2p-discovery-announcer");
        announceThread.setDaemon(true);
        announceThread.start();
    }

    private void announcePresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(DISCOVERY_ADDRESS);
            byte[] data = (HELLO_PREFIX + nodeId).getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, DISCOVERY_PORT);
            socket.send(packet);
        } catch (IOException ignored) {
            // Discovery is best-effort; manual IP still works.
        }
    }

    private void addPeer(String ip) {
        if (isLocalAddress(ip)) {
            return;
        }
        if (peers.add(ip)) {
            SwingUtilities.invokeLater(() -> peerCountListener.accept(peers.size()));
        }
    }

    private boolean isLocalAddress(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                            .flatMap(netIf -> Collections.list(netIf.getInetAddresses()).stream())
                            .anyMatch(address::equals);
        } catch (Exception e) {
            return false;
        }
    }

    public void sendDraw(int x1, int y1, int x2, int y2, Color color, int size) {
        sendToPeers("DRAW|" + x1 + "|" + y1 + "|" + x2 + "|" + y2 + "|"
                + color.getRed() + "|" + color.getGreen() + "|" + color.getBlue() + "|" + size);
    }

    public void sendClear() {
        sendToPeers("CLEAR");
    }

    public void sendChat(String message) {
        sendToPeers("CHAT|" + encode(username) + "|" + encode(message));
    }

    public void sendTextSnapshot(String snapshot) {
        sendToPeers("TEXTS|" + encode(snapshot));
    }

    private void sendHello(String peerIp) {
        sendData(peerIp, "HELLO|" + encode(username));
    }

    private void sendToPeers(String data) {
        for (String peer : peers) {
            sendData(peer, data);
        }
    }

    private void sendData(String peerIp, String data) {
        Thread senderThread = new Thread(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(peerIp, TCP_PORT), 1500);
                try (PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                    out.println(data);
                }
            } catch (IOException ignored) {
                // Peer may be offline; discovery/manual entry can find it again later.
            }
        }, "p2p-sender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

