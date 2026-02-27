package com.oms.gatewayservice.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam String userId) {
        
        String token = jwtUtil.generateToken(userId);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
