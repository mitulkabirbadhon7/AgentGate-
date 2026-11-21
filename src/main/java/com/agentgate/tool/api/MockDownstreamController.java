package com.agentgate.tool.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock")
public class MockDownstreamController {

    @PostMapping("/weather")
    public ResponseEntity<Map<String, Object>> mockWeather(@RequestBody(required = false) Map<String, Object> body) {
        String city = (body != null && body.containsKey("city")) ? String.valueOf(body.get("city")) : "London";
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "city", city,
                "temperature_c", 21.5,
                "condition", "Partly Cloudy",
                "governance", "AgentGate Zero-Trust Protected",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> mockEcho(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "received_payload", body != null ? body : Map.of(),
                "governance", "AgentGate Zero-Trust Protected",
                "timestamp", Instant.now().toString()
        ));
    }
}
