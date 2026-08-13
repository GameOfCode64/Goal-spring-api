package com.tracker.backend.service;

import com.tracker.backend.dto.TrackingTickResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Fans out a budget alert to whichever channels can reach the user right
 * now. WebSocket only reaches a client with an open connection (desktop,
 * typically) - FCM reaches mobile even if the app is backgrounded. Both
 * are attempted independently; either failing should never break the
 * calling request or block the other channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDeliveryService {

    private final SimpMessagingTemplate messagingTemplate;
    private final FcmPushService fcmPushService;

    public void pushToUser(Long userId, TrackingTickResponse.BudgetAlert alert) {
        pushWebSocket(userId, alert);
        pushFcm(userId, alert);
    }

    private void pushWebSocket(Long userId, TrackingTickResponse.BudgetAlert alert) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/alerts",
                    alert
            );
            log.info("Pushed WebSocket alert to user {}: {} {}", userId, alert.category(), alert.tier());
        } catch (Exception e) {
            log.debug("No active WebSocket session for user {} (or push failed): {}", userId, e.getMessage());
        }
    }

    private void pushFcm(Long userId, TrackingTickResponse.BudgetAlert alert) {
        String title = "Time limit reached";
        String body = "You've used %d min of your %d min %s limit today.".formatted(
                alert.usedMinutes(), alert.limitMinutes(), alert.category()
        );
        fcmPushService.pushToUser(userId, title, body);
    }
}