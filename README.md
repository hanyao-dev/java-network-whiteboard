# Java Network Whiteboard

A production-oriented Java Core portfolio project that demonstrates a collaborative whiteboard built with TCP sockets, UDP multicast discovery, multithreading, Swing UI, and a small application-layer protocol.

## Project Overview

Java Network Whiteboard is a multi-user drawing and chat application. One instance can run as a TCP server, and other instances connect as TCP clients. Drawing events, chat messages, join/leave events, and clear-board commands are encoded as protocol messages and synchronized over reliable TCP connections. UDP multicast is used only for local peer discovery beacons.

## Features

- Multi-user whiteboard drawing synchronization
- Chat synchronization
- Join/leave peer state tracking
- TCP client/server architecture
- UDP multicast peer discovery
- Application-layer message protocol with escaping
- Thread-safe peer, drawing, and chat services
- Graceful socket and executor shutdown
- Swing UI separated from network code
- Unit tests for protocol codec and peer service

## Tech Stack

- Java 17
- Maven
- Java TCP Socket / ServerSocket
- Java UDP MulticastSocket
- ExecutorService
- ConcurrentHashMap and CopyOnWriteArrayList
- Swing
- JUnit 5

## Architecture

```text
src/main/java/com/hanyao/whiteboard
├── Main.java
├── config
├── model
├── protocol
├── network
├── service
├── ui
└── util
```

The UI does not parse strings and does not open sockets. It calls service-level operations and sends typed `Message` objects through a `MessageSender`. The network layer reads/writes encoded lines, while `MessageCodec` owns all wire-format parsing.

## TCP / UDP Design

TCP is used for reliable real-time application events:

- `JOIN`
- `LEAVE`
- `DRAW`
- `CHAT`
- `CLEAR`
- `PING`
- `PONG`

UDP is used only for LAN discovery. Each server instance periodically multicasts a `JOIN|username|ip|port` beacon so peers can discover available whiteboard hosts without sending drawing data over UDP.

## Message Protocol

Messages are line-delimited UTF-8 strings:

```text
JOIN|username|ip|port
LEAVE|username
DRAW|x1|y1|x2|y2|color|strokeWidth
CHAT|username|message
CLEAR|username
PING|username
PONG|username
```

The codec supports escaping for `|`, backslash, and newlines, so chat payloads can contain normal user text without breaking parsing.

## Concurrency Model

- `TcpServer` uses a single accept thread, a cached client handler pool, and a bounded send pool.
- `TcpClient` uses one listener thread and one send queue.
- `UdpDiscoveryService` uses one listener thread and one scheduled beacon executor.
- Shared peer state is stored in `ConcurrentHashMap`.
- UI updates are marshalled back to the Swing event dispatch thread.
- Shutdown closes sockets and stops executor services.

## How to Run

Build and test:

```bash
mvn clean test
```

Run a server:

```bash
mvn exec:java -Dexec.args="--server 5050 alice"
```

Run a client:

```bash
mvn exec:java -Dexec.args="--connect localhost 5050 bob"
```

The default TCP port is `5050`.

## Project Structure

- `model`: domain records such as peers, draw commands, and chat messages
- `protocol`: message types and codec
- `network`: TCP server/client handlers and UDP discovery
- `service`: thread-safe application state
- `ui`: Swing frame, whiteboard panel, and chat panel
- `util`: message dispatch and conversion helpers
- `docs`: design notes for architecture, protocol, networking, and interviews

## Future Improvements

- Add host selection UI using discovered peers
- Add reconnect logic and heartbeat timeout handling
- Persist drawing history for late-joining clients
- Add undo/redo and richer drawing tools
- Add integration tests with loopback sockets
- Add structured logging configuration
- Package with Maven Shade or jlink

## Resume Highlights

- Designed a Java 17 collaborative whiteboard using TCP sockets for reliable synchronization and UDP multicast for peer discovery.
- Implemented a custom application-layer protocol with a dedicated codec, escaping, validation, and unit tests.
- Built a layered client/server architecture separating Swing UI, services, network transport, and protocol parsing.
- Applied Java concurrency primitives including `ExecutorService`, `ConcurrentHashMap`, and graceful shutdown handling.
