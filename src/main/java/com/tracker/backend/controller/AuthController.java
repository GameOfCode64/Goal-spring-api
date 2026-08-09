package com.tracker.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TEMPORARY - real client apps (Electron/React Native) will register their
 * own deep-link URL scheme as the OAuth2 redirect target instead of this.
 * This exists only so you can manually see and copy the issued JWT during
 * backend development/testing.
 */
@RestController
public class AuthController {

    @GetMapping("/auth/success")
    public Map<String, String> authSuccess(@RequestParam String token) {
        return Map.of(
                "message", "Login successful. Copy the token below and use it as a Bearer token.",
                "token", token
        );
    }
}