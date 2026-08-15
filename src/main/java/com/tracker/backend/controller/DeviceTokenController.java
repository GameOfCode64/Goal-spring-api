package com.tracker.backend.controller;

import com.tracker.backend.dto.DeviceTokenRequest;
import com.tracker.backend.entity.DeviceToken;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.DeviceTokenRepository;
import com.tracker.backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Called once by React Native on app startup (and again whenever the OS
 * issues a new FCM token, which happens occasionally) to register the
 * device for push notifications. userId comes from the JWT, not the
 * request body, so a client can't register a token against another user.
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceTokenRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        DeviceToken deviceToken = deviceTokenRepository.findByDeviceToken(request.deviceToken())
                .map(existing -> {
                    existing.setLastUsedAt(OffsetDateTime.now());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .user(user)
                        .deviceToken(request.deviceToken())
                        .platform(request.platform())
                        .build()
                );

        deviceTokenRepository.save(deviceToken);
        return ResponseEntity.ok().build();
    }
}