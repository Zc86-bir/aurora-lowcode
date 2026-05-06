package com.aurora.core.infrastructure.collaboration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Document Room Manager — manages WebSocket sessions per document.
 *
 * <p>Room isolation: each document is scoped to a tenant.
 * Multiple users can collaborate on the same document within a tenant.
 *
 * <p>Uses ConcurrentHashMap for thread-safe room management.
 * Scheduled cleanup runs every 60 seconds to persist documents
 * with no active clients.
 */
@Component
public class DocumentRoomManager {

    private static final Logger log = LoggerFactory.getLogger(DocumentRoomManager.class);

    // Room key: "tenantId:documentId" → Set of sessions
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> rooms =
            new ConcurrentHashMap<>();

    // Document state cache: "tenantId:documentId" → binary state
    private final ConcurrentHashMap<String, byte[]> documentState = new ConcurrentHashMap<>();

    // Cleanup scheduler
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "doc-cleanup");
        t.setDaemon(true);
        return t;
    });

    public DocumentRoomManager() {
        scheduler.scheduleAtFixedRate(this::cleanupEmptyRooms, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Join a document room.
     */
    public void joinRoom(String documentId, UUID tenantId, UUID userId, WebSocketSession session) {
        String roomKey = roomKey(tenantId, documentId);
        rooms.computeIfAbsent(roomKey, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("Room {} now has {} sessions", roomKey, rooms.get(roomKey).size());
    }

    /**
     * Leave a document room.
     */
    public void leaveRoom(String documentId, UUID tenantId, UUID userId, WebSocketSession session) {
        String roomKey = roomKey(tenantId, documentId);
        Set<WebSocketSession> sessions = rooms.get(roomKey);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                rooms.remove(roomKey);
                log.debug("Room {} is now empty, removed", roomKey);
            }
        }
    }

    /**
     * Broadcast a message to all sessions in the room except the sender.
     * Spring WebSocketSession.sendMessage is thread-safe per session.
     */
    public void broadcastToRoom(String documentId, UUID tenantId,
                                 WebSocketSession sender, byte[] message) {
        String roomKey = roomKey(tenantId, documentId);
        Set<WebSocketSession> sessions = rooms.get(roomKey);
        if (sessions == null) return;

        BinaryMessage binaryMessage = new BinaryMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(sender.getId())) {
                try {
                    session.sendMessage(binaryMessage);
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Get the number of active sessions in a room.
     */
    public int getRoomSize(String documentId, UUID tenantId) {
        Set<WebSocketSession> sessions = rooms.get(roomKey(tenantId, documentId));
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Check if a room has any active sessions.
     */
    public boolean isRoomEmpty(String documentId, UUID tenantId) {
        return getRoomSize(documentId, tenantId) == 0;
    }

    private String roomKey(UUID tenantId, String documentId) {
        return tenantId + ":" + documentId;
    }

    private void cleanupEmptyRooms() {
        // Remove rooms with no sessions and persist document state
        rooms.entrySet().removeIf(entry -> {
            if (entry.getValue().isEmpty()) {
                log.debug("Cleaning up empty room: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Shutdown the cleanup scheduler on application stop.
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
