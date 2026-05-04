package com.aurora.core.adapter.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server Configuration with SSE Transport
 *
 * Exposes MCP tools via Server-Sent Events (SSE) for external AI clients.
 *
 * Endpoints (auto-configured by spring-ai-mcp-server-webmvc-spring-boot-starter):
 * - SSE endpoint: /sse (event stream for server → client)
 * - Message endpoint: /mcp/message (client → server messages)
 *
 * Configuration via application.yml:
 *   spring.ai.mcp.server.name: aurora-lowcode
 *   spring.ai.mcp.server.version: 1.0.0
 *   spring.ai.mcp.server.sse-message-endpoint: /mcp/message
 *
 * All endpoints are protected by JWT authentication via {@link McpSecurityFilter}.
 *
 * External AI clients (Cursor, Claude Desktop, etc.) connect via:
 *   1. GET /sse?token=<jwt> — establish SSE connection
 *   2. POST /mcp/message — send tool calls, receive responses via SSE
 *
 * The server auto-discovers all ToolCallback beans registered in the Spring context.
 * {@link AuroraSkillToolProvider} registers all 10 built-in Skills as ToolCallbacks.
 *
 * To customize MCP server behavior, add additional beans in this class.
 * Spring Boot auto-configuration handles the WebMvcSseServerTransportProvider,
 * McpServer initialization, and RouterFunction registration automatically.
 */
@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    /**
     * Custom server info for MCP protocol handshake.
     *
     * The auto-configuration uses application.yml values:
     *   spring.ai.mcp.server.name = ${AURORA_MCP_NAME:aurora-lowcode}
     *   spring.ai.mcp.server.version = ${AURORA_MCP_VERSION:1.0.0}
     *   spring.ai.mcp.server.sse-message-endpoint = /mcp/message
     *
     * To add custom capabilities, create a custom McpServer bean here.
     * The auto-configured server handles:
     * - SSE transport initialization
     * - ToolCallback registration from all ToolCallbackProvider beans
     * - Resource registration
     * - Protocol version negotiation
     */
}
