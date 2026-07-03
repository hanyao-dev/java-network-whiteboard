package com.hanyao.whiteboard.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageCodecTest {
    @Test
    void encodesAndDecodesDrawMessage() {
        Message message = Message.draw("10", "20", "30", "40", "#FF0000", "3");

        String encoded = MessageCodec.encode(message);
        Message decoded = MessageCodec.decode(encoded);

        assertEquals(message, decoded);
    }

    @Test
    void preservesPipeAndNewlineInsideChatMessage() {
        Message message = Message.chat("alice", "hello | team\nnew line");

        Message decoded = MessageCodec.decode(MessageCodec.encode(message));

        assertEquals("hello | team\nnew line", decoded.fields().get(1));
    }

    @Test
    void rejectsMessagesWithMissingFields() {
        assertThrows(IllegalArgumentException.class, () -> MessageCodec.decode("JOIN|alice|127.0.0.1"));
    }
}
