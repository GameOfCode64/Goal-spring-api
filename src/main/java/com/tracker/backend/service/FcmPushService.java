package com.tracker.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.tracker.backend.entity.DeviceToken;
import com.tracker.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sends the "Get back to work" push notification to every device a user
 * has registered (they may have more than one). Failures for individual
 * devices are logged and skipped rather than failing the whole batch -
 * a stale/uninstalled-app token on one device shouldn't block push to
 * the user's other devices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;

    public void pushToUser(Long userId, String title, String body) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);

        if (tokens.isEmpty()) {
            log.debug("No registered devices for user {} - skipping FCM push", userId);
            return;
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getDeviceToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("Pushed FCM notification to user {} device {}", userId, deviceToken.getId());

            } catch (FirebaseMessagingException e) {
                // A token can go stale (app uninstalled, token rotated) -
                // log and move on rather than throwing. Cleaning up dead
                // tokens can be a separate scheduled job later.
                log.warn("FCM push failed for user {} device {}: {}", userId, deviceToken.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during FCM push for user {}: {}", userId, e.getMessage());
            }
        }
    }
}