package com.tracker.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tracker.backend.entity.DeviceType;

/**
 * Sent by Electron/React Native/browser clients once per CLOSED interval
 * (i.e. when the user switches away from an app/tab) - not on every raw
 * tick. The client is responsible for measuring durationSeconds locally
 * and only calling this endpoint when an interval actually ends.
 */
public record TrackingTickRequest(
        @NotNull Long userId,
        @NotNull DeviceType deviceType,
        @NotBlank String appName,
        String windowTitle,
        String url,
        @NotNull @Min(1) Integer durationSeconds
) {
    public ClassificationRequest toClassificationRequest() {
        return new ClassificationRequest(appName, windowTitle, url);
    }
}