package com.aurora.core.adapter.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/v1")
public class DevApiHealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "status", "UP",
                        "timestamp", Instant.now().toString()
                )
        ));
    }
}
