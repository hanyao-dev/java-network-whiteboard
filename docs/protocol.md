# Protocol

## Wire Format

Every message is a single UTF-8 line:

```text
TYPE|field1|field2|...
```

`MessageCodec` is the only class that encodes and decodes the wire format. Network classes work with typed `Message` objects instead of calling `split`.

## Message Types

```text
JOIN|username|ip|port
LEAVE|username
DRAW|x1|y1|x2|y2|color|strokeWidth
CHAT|username|message
CLEAR|username
PING|username
PONG|username
```

## Escaping

The codec escapes:

- `|`
- `\`
- newline
- carriage return

This lets chat messages contain user text such as `hello | team` without corrupting the frame.

## Validation

`MessageType` defines the expected field count. The `Message` record validates field count at construction time, so malformed frames are rejected close to the protocol boundary.
