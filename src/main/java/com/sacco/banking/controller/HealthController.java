// File: src/main/java/com/sacco/banking/controller/HealthController.java
package com.sacco.banking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = {"Authorization", "Content-Type", "Accept", "X-Requested-With"},
        allowCredentials = "true",
        maxAge = 3600
)
public class HealthController {

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "SACCO Banking API");
        response.put("version", "1.0.0");

        return ResponseEntity.ok(response);
    }

    // Explicitly handle OPTIONS requests
    @RequestMapping(value = "/health", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptionsHealth() {
        return ResponseEntity.ok().build();
    }
}