package com.hanyao.whiteboard.network;

import com.hanyao.whiteboard.config.AppConfig;
import com.hanyao.whiteboard.model.Peer;
import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.protocol.MessageCodec;
import com.hanyao.whiteboard.protocol.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UdpDiscoveryService implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(UdpDiscoveryService.class.getName());
    private static final int BUFFER_SIZE = 1024;
    private static final int BEACON_INTERVAL_SECONDS = 5;

    private final String username;
    private final int tcpPort;
    private final Consumer<Peer> peerConsumer;
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService beaconExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private MulticastSocket socket;
    private InetAddress group;
    private NetworkInterface networkInterface;

    public UdpDiscoveryService(String username, int tcpPort, Consumer<Peer> peerConsumer) {
        this.username = username;
        this.tcpPort = tcpPort;
        this.peerConsumer = peerConsumer;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        group = InetAddress.getByName(AppConfig.UDP_MULTICAST_GROUP);
        socket = new MulticastSocket(AppConfig.UDP_DISCOVERY_PORT);
        socket.setReuseAddress(true);
        networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        socket.joinGroup(new java.net.InetSocketAddress(group, AppConfig.UDP_DISCOVERY_PORT), networkInterface);
        listenerExecutor.submit(this::listen);
        beaconExecutor.scheduleAtFixedRate(this::sendBeacon, 0, BEACON_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void listen() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String payload = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                Message message = MessageCodec.decode(payload);
                if (message.type() == MessageType.JOIN && !message.fields().get(0).equals(username)) {
                    peerConsumer.accept(toPeer(message, packet.getAddress().getHostAddress()));
                }
            } catch (IOException ex) {
                if (running.get()) {
                    LOGGER.log(Level.FINE, "UDP discovery receive failed", ex);
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.FINE, "Ignoring invalid UDP discovery packet", ex);
            }
        }
    }

    private Peer toPeer(Message message, String fallbackHost) {
        String host = message.fields().get(1).isBlank() ? fallbackHost : message.fields().get(1);
        int port = Integer.parseInt(message.fields().get(2));
        return new Peer(message.fields().get(0), host, port, Instant.now());
    }

    public void sendBeacon() {
        if (!running.get()) {
            return;
        }
        try {
            Message message = Message.join(username, InetAddress.getLocalHost().getHostAddress(), tcpPort);
            byte[] payload = MessageCodec.encode(message).getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(payload, payload.length, group, AppConfig.UDP_DISCOVERY_PORT);
            socket.send(packet);
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Failed to send UDP discovery beacon", ex);
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (socket != null) {
            try {
                socket.leaveGroup(new java.net.InetSocketAddress(group, AppConfig.UDP_DISCOVERY_PORT), networkInterface);
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Failed to leave multicast group", ex);
            }
            socket.close();
        }
        listenerExecutor.shutdownNow();
        beaconExecutor.shutdownNow();
    }
}
