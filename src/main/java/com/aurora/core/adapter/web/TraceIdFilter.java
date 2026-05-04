package com.aurora.core.adapter.web;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Trace ID Filter — Injects TraceId and SpanId into SLF4J MDC.
 *
 * This enables Logs -> Traces correlation in Grafana:
 * - Every log line includes traceId and spanId
 * - Loki's Promtail extracts these from structured logs
 * - Grafana can navigate from a log entry directly to the Tempo trace
 *
 * MDC keys:
 * - traceId: OpenTelemetry trace identifier (hex)
 * - spanId: OpenTelemetry span identifier (hex)
 * - tenantId: Current request tenant (if available)
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract trace/span from current OpenTelemetry context
            Span currentSpan = Span.current();
            if (currentSpan.getSpanContext().isValid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();

                MDC.put(TRACE_ID_KEY, traceId);
                MDC.put(SPAN_ID_KEY, spanId);
            }

            // Extract tenant ID from header for log correlation
            String tenantId = request.getHeader("X-Tenant-Id");
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
