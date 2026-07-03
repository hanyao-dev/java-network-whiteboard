package com.hanyao.whiteboard.service;

import com.hanyao.whiteboard.model.Peer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class PeerService {
    private static final Duration STALE_PEER_THRESHOLD = Duration.ofSeconds(30);

    private final ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<Collection<Peer>>> listeners = new CopyOnWriteArrayList<>();

    public void upsert(Peer peer) {
        peers.put(peer.id(), peer);
        notifyListeners();
    }

    public void remove(String username) {
        peers.entrySet().removeIf(entry -> entry.getValue().username().equals(username));
        notifyListeners();
    }

    public void removeStalePeers() {
        Instant cutoff = Instant.now().minus(STALE_PEER_THRESHOLD);
        peers.entrySet().removeIf(entry -> entry.getValue().lastSeen().isBefore(cutoff));
        notifyListeners();
    }

    public List<Peer> listPeers() {
        return peers.values().stream()
                .sorted((left, right) -> left.username().compareToIgnoreCase(right.username()))
                .toList();
    }

    public void addListener(Consumer<Collection<Peer>> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        List<Peer> snapshot = listPeers();
        listeners.forEach(listener -> listener.accept(snapshot));
    }
}
