package com.aurora.core.adapter.websocket;

import com.aurora.core.infrastructure.collaboration.DocumentRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Yjs WebSocket Handler — processes binary CRDT protocol.
 *
 * <p>Yjs uses a binary protocol for document synchronization.
 * This handler processes sync steps, updates, and awareness messages.
 *
 * <p>Message types (Yjs protocol):
 * <ul>
 *   <li>0: MessageSync — document sync step</li>
 *   <li>1: MessageAwareness — cursor/selection awareness</li>
 *   <li>2: MessageQueryAwareness</li>
 *   <li>3: MessageSync (step 2)</li>
 * </ul>
 */
public class YjsWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(YjsWebSocketHandler.class);

    private static final byte MSG_SYNC = 0;
    private static final byte MSG_AWARENESS = 1;

    private final DocumentRoomManager roomManager;

    public YjsWebSocketHandler(DocumentRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String documentId = extractDocumentId(session);
        UUID tenantId = extractTenantId(session);
        UUID userId = extractUserId(session);

        if (documentId == null || tenantId == null || userId == null) {
            log.warn("WebSocket connection missing required attributes, closing: {}", session.getId());
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
                log.debug("Error closing session: {}", e.getMessage());
            }
            return;
        }

        roomManager.joinRoom(documentId, tenantId, userId, session);
        log.info("User {} joined room '{}' for document {}", userId, documentId, documentId);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String documentId = extractDocumentId(session);
        UUID tenantId = extractTenantId(session);

        if (documentId == null || tenantId == null) {
            return;
        }

        ByteBuffer buffer = message.getPayload();
        if (buffer.remaining() < 1) {
            return;
        }

        byte messageType = buffer.get();

        switch (messageType) {
            case MSG_SYNC -> {
                // Broadcast full sync payload to all other clients in the room
                byte[] fullPayload = new byte[1 + buffer.remaining()];
                fullPayload[0] = MSG_SYNC;
                buffer.get(fullPayload, 1, fullPayload.length - 1);

                roomManager.broadcastToRoom(documentId, tenantId, session, fullPayload);
            }
            case MSG_AWARENESS -> {
                // Broadcast full awareness payload (cursor positions, selections)
                byte[] fullPayload = new byte[1 + buffer.remaining()];
                fullPayload[0] = MSG_AWARENESS;
                buffer.get(fullPayload, 1, fullPayload.length - 1);

                roomManager.broadcastToRoom(documentId, tenantId, session, fullPayload);
            }
            default -> log.trace("Unknown Yjs message type: {} in room {}", messageType, documentId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String documentId = extractDocumentId(session);
        UUID tenantId = extractTenantId(session);
        UUID userId = extractUserId(session);

        if (documentId != null && tenantId != null && userId != null) {
            roomManager.leaveRoom(documentId, tenantId, userId, session);
            log.info("User {} left room '{}' ({})", userId, documentId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
    }

    private String extractDocumentId(WebSocketSession session) {
        Object docId = session.getAttributes().get("documentId");
        return docId != null ? docId.toString() : null;
    }

    private UUID extractTenantId(WebSocketSession session) {
        Object tenantId = session.getAttributes().get("tenantId");
        return tenantId != null ? UUID.fromString(tenantId.toString()) : null;
    }

    private UUID extractUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? UUID.fromString(userId.toString()) : null;
    }
}
