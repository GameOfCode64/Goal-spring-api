package com.tracker.backend.service;

import com.tracker.backend.dto.ClassificationRequest;
import com.tracker.backend.entity.Tag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Tier 3 of the classification funnel - only called on a true cache miss.
 * Kept deliberately isolated so the rest of the app never touches Gemini's
 * HTTP/JSON shape directly; ClassificationService only sees a Tag back.
 */
@Slf4j
@Component
public class GeminiClassifierClient {

    private static final String SYSTEM_INSTRUCTION = """
        You are a fast, deterministic activity classifier. Given a single window
        title, tab title, or app name, output exactly one label from this fixed
        taxonomy: STUDY, WORK, ENTERTAINMENT, WASTE, NEUTRAL.

        Rules:
        - STUDY: educational content, courses, documentation, textbooks, research papers.
        - WORK: professional tools, IDEs, work-related documents/emails/meetings.
        - ENTERTAINMENT: video/social/streaming/gaming content consumed for leisure.
        - WASTE: aimless browsing, infinite-scroll feeds, content with no clear
          productive or restful intent.
        - NEUTRAL: system UI, file managers, ambiguous or insufficient signal.

        Output ONLY the single label word. No explanation, no punctuation, no JSON.
        If uncertain, choose the closest match - never invent a new label.
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // NEVER hardcode the actual key value here. This is populated from the
    // GEMINI_API_KEY environment variable via application.yml's
    // ${GEMINI_API_KEY} placeholder.
    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    public GeminiClassifierClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Returns null on any failure (timeout, malformed response, unknown
     * label) so the caller can decide the fallback behavior rather than
     * this class making that policy decision.
     */
    public Tag classify(ClassificationRequest request) {
        try {
            Map<String, Object> body = buildRequestBody(request);

            String rawResponse = restClient.post()
                    .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String label = extractLabel(rawResponse);
            return Tag.valueOf(label.trim().toUpperCase());

        } catch (Exception e) {
            log.warn("Gemini classification failed for pattern '{}': {}",
                    request.normalizedPattern(), e.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildRequestBody(ClassificationRequest request) {
        String userTurn = """
            App: %s
            Title: %s
            URL: %s
            """.formatted(
                request.appName(),
                request.windowTitle() != null ? request.windowTitle() : "N/A",
                request.url() != null ? request.url() : "N/A"
        );

        return Map.of(
                "system_instruction", Map.of(
                        "parts", new Object[]{ Map.of("text", SYSTEM_INSTRUCTION) }
                ),
                "contents", new Object[]{
                        Map.of("parts", new Object[]{ Map.of("text", userTurn) })
                },
                "generationConfig", Map.of("temperature", 0.0)
        );
    }

    private String extractLabel(String rawResponse) throws Exception {
        tools.jackson.databind.JsonNode root = objectMapper.readTree(rawResponse);
        return root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }
}