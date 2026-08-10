package com.tracker.backend.dto;

import java.util.List;

/**
 * Matches the JSON schema the 9PM coach prompt is instructed to return.
 * Parsed directly from Gemini's response text.
 */
public record CoachReportResult(
        String headline,
        String verdict,          // "STRONG" | "MIXED" | "WEAK"
        List<String> wins,
        List<String> leaks,
        String goalComplianceNote,
        String tomorrowFocus
) {
}
