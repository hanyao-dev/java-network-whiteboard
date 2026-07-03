package com.hanyao.whiteboard.service;

import com.hanyao.whiteboard.model.Peer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeerServiceTest {
    @Test
    void upsertsAndListsPeers() {
        PeerService service = new PeerService();
        Peer peer = new Peer("alice", "127.0.0.1", 5050, Instant.now());

        service.upsert(peer);

        assertEquals(1, service.listPeers().size());
        assertEquals(peer, service.listPeers().get(0));
    }

    @Test
    void removesPeerByUsername() {
        PeerService service = new PeerService();
        service.upsert(new Peer("alice", "127.0.0.1", 5050, Instant.now()));
        service.upsert(new Peer("bob", "127.0.0.2", 5050, Instant.now()));

        service.remove("alice");

        assertEquals(1, service.listPeers().size());
        assertEquals("bob", service.listPeers().get(0).username());
    }

    @Test
    void removesStalePeers() {
        PeerService service = new PeerService();
        service.upsert(new Peer("old", "127.0.0.1", 5050, Instant.now().minusSeconds(60)));
        service.upsert(new Peer("fresh", "127.0.0.2", 5050, Instant.now()));

        service.removeStalePeers();

        assertEquals(1, service.listPeers().size());
        assertEquals("fresh", service.listPeers().get(0).username());
    }

    @Test
    void notifiesListenersWithSnapshots() {
        PeerService service = new PeerService();
        final int[] count = {0};
        service.addListener(peers -> count[0] = peers.size());

        service.upsert(new Peer("alice", "127.0.0.1", 5050, Instant.now()));

        assertTrue(count[0] > 0);
    }
}
