package com.tracker.backend.service;

import com.tracker.backend.dto.ClassificationResult;
import com.tracker.backend.dto.TrackingTickRequest;
import com.tracker.backend.dto.TrackingTickResponse;
import com.tracker.backend.entity.LimitConfig;
import com.tracker.backend.entity.ScopeType;
import com.tracker.backend.entity.TimelineActivity;
import com.tracker.backend.entity.User;
import com.tracker.backend.repository.LimitConfigRepository;
import com.tracker.backend.repository.TimelineActivityRepository;
import com.tracker.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates a single tracking tick end to end:
 *   1. Classify the activity (3-tier funnel)
 *   2. Increment today's Redis usage counter for that category
 *   3. Check configured limits - fire alert flags if just crossed
 *   4. Persist the closed interval to Postgres (system of record)
 *
 * This is the method the real client apps (Electron/React Native/browser)
 * will call on every closed interval, once wired to a controller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final ClassificationService classificationService;
    private final RedisUsageService redisUsageService;
    private final UserRepository userRepository;
    private final LimitConfigRepository limitConfigRepository;
    private final TimelineActivityRepository timelineActivityRepository;
    private final AlertDeliveryService alertDeliveryService;

    @Transactional
    public TrackingTickResponse processTick(TrackingTickRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + request.userId()));

        ZoneId userZone = ZoneId.of(user.getTimezone());

        // ---------- 1. Classify ----------
        ClassificationResult classification = classificationService.classify(request.toClassificationRequest());
        String category = classification.tag().name();

        // ---------- 2. Increment Redis usage ----------
        long newTotalSeconds = redisUsageService.incrementCategoryUsage(
                user.getId(), category, request.durationSeconds(), userZone
        );
        long newTotalMinutes = newTotalSeconds / 60;

        // ---------- 3. Check configured limits ----------
        List<TrackingTickResponse.BudgetAlert> alerts = checkLimitsAndCollectAlerts(
                user, category, newTotalMinutes, userZone
        );

        // ---------- 4. Persist to Postgres ----------
        saveTimelineActivity(user, request, classification, newTotalSeconds);

        return new TrackingTickResponse(
                classification.tag(),
                classification.source(),
                newTotalSeconds,
                alerts
        );
    }

    private List<TrackingTickResponse.BudgetAlert> checkLimitsAndCollectAlerts(
            User user, String category, long usedMinutes, ZoneId userZone
    ) {
        List<TrackingTickResponse.BudgetAlert> alerts = new ArrayList<>();

        limitConfigRepository.findByUserIdAndScopeTypeAndScopeValue(user.getId(), ScopeType.CATEGORY, category)
                .filter(LimitConfig::getIsActive)
                .ifPresent(limit -> {
                    int limitMinutes = limit.getDailyLimitMinutes();
                    int warnThresholdMinutes = (limitMinutes * limit.getWarnThresholdPct()) / 100;

                    if (usedMinutes >= limitMinutes) {
                        if (redisUsageService.shouldFireBudgetAlert(user.getId(), category, "EXCEEDED_100", userZone)) {
                            var alert = new TrackingTickResponse.BudgetAlert(category, "EXCEEDED_100", limitMinutes, usedMinutes);
                            alerts.add(alert);
                            alertDeliveryService.pushToUser(user.getId(), alert);
                            log.info("User {} exceeded {} limit: {}/{} min", user.getId(), category, usedMinutes, limitMinutes);
                        }
                    } else if (usedMinutes >= warnThresholdMinutes) {
                        if (redisUsageService.shouldFireBudgetAlert(user.getId(), category, "WARN_80", userZone)) {
                            var alert = new TrackingTickResponse.BudgetAlert(category, "WARN_80", limitMinutes, usedMinutes);
                            alerts.add(alert);
                            alertDeliveryService.pushToUser(user.getId(), alert);
                            log.info("User {} hit warning threshold for {}: {}/{} min", user.getId(), category, usedMinutes, limitMinutes);
                        }
                    }
                });

        return alerts;
    }

    private void saveTimelineActivity(
            User user, TrackingTickRequest request, ClassificationResult classification, long newTotalSecondsToday
    ) {
        OffsetDateTime endedAt = OffsetDateTime.now();
        OffsetDateTime startedAt = endedAt.minusSeconds(request.durationSeconds());

        TimelineActivity activity = TimelineActivity.builder()
                .user(user)
                .deviceType(request.deviceType())
                .appName(request.appName())
                .windowTitle(request.windowTitle())
                .url(request.url())
                .tag(classification.tag())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationSeconds(request.durationSeconds())
                .localDate(endedAt.atZoneSameInstant(ZoneId.of(user.getTimezone())).toLocalDate())
                .build();

        timelineActivityRepository.save(activity);
    }
}