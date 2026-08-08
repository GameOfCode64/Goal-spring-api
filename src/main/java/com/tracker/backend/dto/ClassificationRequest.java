package com.tracker.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Normalized shape that desktop/mobile/browser ticks all get mapped into
 * before hitting the classification funnel. appName is required; title/url
 * are optional since not every source has both (e.g. a plain desktop app
 * window has a title but no url).
 */
public record ClassificationRequest(
        @NotBlank String appName,
        String windowTitle,
        String url
) {
    /**
     * The exact string the classification funnel keys off of. Combining
     * appName + title (not url) keeps the cache pattern stable across
     * URL params changing on the same logical page (e.g. YouTube video IDs).
     */
    public String normalizedPattern() {
        String title = windowTitle != null ? windowTitle.trim() : "";
        return (appName.trim() + " | " + title).toLowerCase();
    }
}

