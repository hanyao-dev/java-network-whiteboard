package com.hanyao.whiteboard.legacy;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 蝚砌??挾嚗DP 蝯蝭暺??
 * ??AirShit ?銵?撖虫??芸??⊥??蝺?銵函雁霅?
 */
public class NodeScanner {
    // ?身蝯雿?????(?航閮?
    private static final String MULTICAST_ADDRESS = "239.255.42.99";
    private static final int PORT = 50000;
    private static final int BUFFER_SIZE = 1024;

    // ?脣??桀??函???暺?(IP -> NodeInfo)
    private final Map<String, Long> onlineNodes = new ConcurrentHashMap<>();
    private final String localIp;

    public NodeScanner() throws Exception {
        this.localIp = InetAddress.getLocalHost().getHostAddress();
    }

    // ???交?瑁?蝺???隤唬?蝺?
    public void startListening() {
        new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(PORT)) {
                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                socket.joinGroup(new InetSocketAddress(group, PORT), netIf);

                System.out.println("UDP ????撌脣???蝑?蝭暺???..");

                byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    String senderIp = packet.getAddress().getHostAddress();

                    if (!senderIp.equals(localIp)) {
                        if (message.startsWith("HELLO")) {
                            System.out.println("?潛?啁?暺? " + senderIp);
                            onlineNodes.put(senderIp, System.currentTimeMillis());
                            // ?嗅?敺?閬?蝣箔?撠銋???函?
                            sendAnnouncement("I_AM_HERE");
                        } else if (message.equals("I_AM_HERE")) {
                            onlineNodes.put(senderIp, System.currentTimeMillis());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ?潮誨?哨??迄憭批振??銝?鈭?
    public void sendAnnouncement(String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ??敹歲瑼Ｘ嚗宏?方???靘? 10 蝘???嚗?蝭暺?
    public void startHeartbeatChecker() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // 瘥?5 蝘炎?乩?甈?
                    long now = System.currentTimeMillis();
                    onlineNodes.entrySet().removeIf(entry -> (now - entry.getValue() > 10000));
                    System.out.println("?桀??函?蝭暺? " + onlineNodes.keySet());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        NodeScanner scanner = new NodeScanner();
        scanner.startListening();
        scanner.startHeartbeatChecker();

        // 璅⊥?潮?蝺
        scanner.sendAnnouncement("HELLO_AIRSHIT_PROJECT");
    }
}
