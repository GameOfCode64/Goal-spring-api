package com.tracker.backend.dto;

import com.tracker.backend.entity.Tag;

import java.util.List;

/**
 * Returned to the client after a tick is processed. alertsToShow is empty
 * in the normal case; when non-empty, the client (Electron/React Native)
 * should surface the corresponding popup/modal immediately.
 */
public record TrackingTickResponse(
        Tag tag,
        String classificationSource,
        long categoryTotalSecondsToday,
        List<BudgetAlert> alertsToShow
) {
    public record BudgetAlert(
            String category,
            String tier,        // "WARN_80" | "EXCEEDED_100"
            int limitMinutes,
            long usedMinutes
    ) {
    }
}