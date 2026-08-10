package com.tracker.backend.service;


import com.tracker.backend.dto.CoachReportResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Calls Gemini once per user per night with the "Executive Productivity
 * Coach" persona. Deliberately a separate client from
 * GeminiClassifierClient - different prompt, different call frequency
 * (once/day vs. every tick), different tuning priorities (quality over
 * latency here).
 */
@Slf4j
@Component
public class CoachReportClient {

    private static final String SYSTEM_INSTRUCTION = """
        You are a strict, no-nonsense executive productivity coach. You have one
        job: review the user's day objectively and tell them the truth about their
        performance, framed for growth, not comfort. Be direct, specific, and brief.
        Avoid generic praise. Reference actual numbers from the data provided.

        Output valid JSON only, matching this schema:
        {
          "headline": string,
          "verdict": "STRONG" | "MIXED" | "WEAK",
          "wins": [string],
          "leaks": [string],
          "goalComplianceNote": string,
          "tomorrowFocus": string
        }
        No text outside the JSON object.
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String model;

    public CoachReportClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * userTurnData should already be a fully-formatted summary of the
     * user's day (goals, time-by-category, limits exceeded, streaks) -
     * see CoachReportService for how it's assembled.
     */
    public CoachReportResult generateReport(String userTurnData) {
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", new Object[]{ Map.of("text", SYSTEM_INSTRUCTION) }
                ),
                "contents", new Object[]{
                        Map.of("parts", new Object[]{ Map.of("text", userTurnData) })
                },
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "responseMimeType", "application/json"
                )
        );

        String rawResponse = restClient.post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        String jsonText = extractText(rawResponse);
        return parseReport(jsonText);
    }

    private String extractText(String rawResponse) {
        try {
            tools.jackson.databind.JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract text from Gemini response: " + rawResponse, e);
        }
    }

    private CoachReportResult parseReport(String jsonText) {
        try {
            return objectMapper.readValue(jsonText, CoachReportResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse coach report JSON: " + jsonText, e);
        }
    }
}