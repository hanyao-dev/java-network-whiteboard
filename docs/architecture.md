# Architecture

## Goals

The project is structured as a small production-quality Java Core networking application. It keeps the networking concepts visible while avoiding a god class or direct UI/socket coupling.

## Layers

```text
UI -> Service -> Protocol Message -> Network
Network -> Protocol Message -> Dispatcher -> Service -> UI listener
```

## Packages

- `config`: central runtime defaults such as ports, multicast group, and timeouts
- `model`: immutable domain records
- `protocol`: protocol enum, message record, and encode/decode logic
- `network`: TCP server, TCP client, client handlers, UDP discovery, sender/listener interfaces
- `service`: thread-safe application state and listener notification
- `ui`: Swing components
- `util`: dispatcher and message conversion helpers

## Runtime Modes

Server mode:

```bash
mvn exec:java -Dexec.args="--server 5050 alice"
```

Client mode:

```bash
mvn exec:java -Dexec.args="--connect localhost 5050 bob"
```

The server accepts TCP clients and rebroadcasts messages. Clients send messages to the server and render broadcasts from the server.
