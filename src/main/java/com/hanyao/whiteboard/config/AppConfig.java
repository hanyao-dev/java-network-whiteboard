package com.hanyao.whiteboard.config;

public final class AppConfig {
    public static final int DEFAULT_TCP_PORT = 5050;
    public static final int UDP_DISCOVERY_PORT = 5051;
    public static final String UDP_MULTICAST_GROUP = "230.0.0.42";
    public static final int SOCKET_CONNECT_TIMEOUT_MILLIS = 3_000;
    public static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    private AppConfig() {
    }
}
