# Interview Notes

## What This Project Demonstrates

- Java socket programming with both TCP and UDP
- Client/server architecture
- Application-layer protocol design
- Concurrency with `ExecutorService`
- Thread-safe shared state
- Swing UI separation from backend/networking concerns
- Graceful shutdown of sockets and listener loops
- Unit testing protocol and service behavior

## Design Tradeoffs

TCP is used for drawing and chat because those events need ordering and reliable delivery. UDP is reserved for discovery because discovery can tolerate packet loss and should not block the main synchronization path.

The protocol is intentionally simple and text-based so it is easy to debug with logs or socket tools. `MessageCodec` centralizes parsing to avoid scattered string handling.

The MVP uses a server relay topology. This is easier to reason about than full mesh peer-to-peer synchronization and is a better starting point for correctness.

## Follow-Up Improvements

- Add sequence numbers or message IDs for deduplication.
- Add ping/pong heartbeats and stale connection cleanup.
- Replay drawing history to late joiners.
- Add integration tests for TCP loopback behavior.
- Add packaging and release scripts.
- Add structured logs and metrics.
