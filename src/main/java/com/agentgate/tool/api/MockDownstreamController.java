package com.agentgate.tool.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock")
public class MockDownstreamController {

    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> mockWeatherGet(
            @RequestParam(required = false, defaultValue = "London") String city) {
        return createWeatherResponse(city);
    }

    @PostMapping("/weather")
    public ResponseEntity<Map<String, Object>> mockWeatherPost(
            @RequestBody(required = false) Map<String, Object> body) {
        String city = (body != null && body.containsKey("city") && body.get("city") != null)
                ? String.valueOf(body.get("city"))
                : "London";
        return createWeatherResponse(city);
    }

    private ResponseEntity<Map<String, Object>> createWeatherResponse(String city) {
        String effectiveCity = (city != null && !city.isBlank()) ? city.trim() : "London";
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "city", effectiveCity,
                "temperature_c", 21.5,
                "condition", "Partly Cloudy",
                "governance", "AgentGate Zero-Trust Protected",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/echo")
    public ResponseEntity<Map<String, Object>> mockEchoGet(
            @RequestParam(required = false, defaultValue = "ping") String message) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "received_payload", Map.of("message", message),
                "governance", "AgentGate Zero-Trust Protected",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> mockEchoPost(
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "received_payload", body != null ? body : Map.of(),
                "governance", "AgentGate Zero-Trust Protected",
                "timestamp", Instant.now().toString()
        ));
    }
}
