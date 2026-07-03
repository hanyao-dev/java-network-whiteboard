# Network Flow

## Server Startup

1. `Main` creates services and `ProtocolDispatcher`.
2. `TcpServer` binds to the configured TCP port.
3. `UdpDiscoveryService` joins the multicast group.
4. The Swing UI is shown.

## Client Startup

1. `Main` creates services and `ProtocolDispatcher`.
2. `TcpClient` connects to the server.
3. Client sends `JOIN|username|ip|localPort`.
4. The Swing UI is shown.

## Drawing Flow

1. User drags the mouse on `WhiteboardPanel`.
2. UI creates a `DrawCommand`.
3. `DrawingService` stores and renders it locally.
4. UI converts it to `DRAW` via `NetworkMessages`.
5. TCP sends it to the server.
6. Server dispatches it locally and broadcasts it to other clients.
7. Remote clients dispatch it into `DrawingService`.

## UDP Discovery Flow

1. Server periodically multicasts `JOIN|username|ip|port`.
2. Other running instances receive the beacon.
3. `PeerService` upserts the discovered peer.
4. UI peer list refreshes from the service listener.

UDP never carries drawing or chat data.
