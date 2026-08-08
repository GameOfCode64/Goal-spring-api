package com.tracker.backend.service;

import com.tracker.backend.dto.ClassificationRequest;
import com.tracker.backend.dto.ClassificationResult;
import com.tracker.backend.entity.CacheSource;
import com.tracker.backend.entity.ClassificationCache;
import com.tracker.backend.entity.Tag;
import com.tracker.backend.repository.ClassificationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * The 3-tier classification funnel described in the blueprint:
 *   Tier 1: exact cache hit (by pattern hash)          - instant, free
 *   Tier 2: fuzzy match against existing cache entries - cheap, no AI call
 *   Tier 3: Gemini API call, result written back to cache so it's
 *           never asked again
 *
 * This keeps AI usage roughly proportional to "genuinely new" titles
 * rather than every tick from every device.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final ClassificationCacheRepository cacheRepository;
    private final GeminiClassifierClient geminiClient;

    // How similar two patterns need to be (0.0-1.0) for Tier 2 to count
    // it as a match rather than falling through to Gemini.
    private static final double FUZZY_MATCH_THRESHOLD = 0.85;

    public ClassificationResult classify(ClassificationRequest request) {
        String pattern = request.normalizedPattern();
        String hash = sha256(pattern);

        // ---------- TIER 1: exact hash match ----------
        Optional<ClassificationCache> exactMatch = cacheRepository.findByPatternHash(hash);
        if (exactMatch.isPresent()) {
            cacheRepository.recordHit(hash);
            return new ClassificationResult(exactMatch.get().getTag(), "CACHE_EXACT", false);
        }

        // ---------- TIER 2: fuzzy match against recent cache entries ----------
        Optional<ClassificationCache> fuzzyMatch = findFuzzyMatch(pattern, request.appName());
        if (fuzzyMatch.isPresent()) {
            cacheRepository.recordHit(fuzzyMatch.get().getPatternHash());
            return new ClassificationResult(fuzzyMatch.get().getTag(), "CACHE_FUZZY", false);
        }

        // ---------- TIER 3: Gemini fallback ----------
        Tag aiTag = geminiClient.classify(request);
        if (aiTag == null) {
            // Gemini failed - fail safe to NEUTRAL rather than blocking the
            // tick entirely or guessing WASTE, which would wrongly ding
            // the user's budget on an infrastructure failure.
            log.warn("Falling back to NEUTRAL for pattern '{}' after Gemini failure", pattern);
            return new ClassificationResult(Tag.NEUTRAL, "AI", false);
        }

        // Write back so this exact pattern is a Tier-1 hit next time.
        ClassificationCache newEntry = ClassificationCache.builder()
                .patternText(pattern)
                .patternHash(hash)
                .tag(aiTag)
                .appName(request.appName())
                .source(CacheSource.AI)
                .build();
        cacheRepository.save(newEntry);

        return new ClassificationResult(aiTag, "AI", true);
    }

    /**
     * Simple, cheap fuzzy match: same app + high character-overlap title
     * against recently-seen patterns for that app. This is intentionally
     * a basic heuristic (Jaccard similarity on word sets) rather than a
     * full similarity-search index - good enough to catch near-duplicate
     * titles (e.g. a YouTube title with slightly different suffix text)
     * without adding infrastructure like a vector store.
     */
    private Optional<ClassificationCache> findFuzzyMatch(String pattern, String appName) {
        List<ClassificationCache> candidates = cacheRepository.findAll().stream()
                .filter(c -> appName.equalsIgnoreCase(c.getAppName()))
                .toList();

        return candidates.stream()
                .filter(c -> jaccardSimilarity(pattern, c.getPatternText()) >= FUZZY_MATCH_THRESHOLD)
                .findFirst();
    }

    private double jaccardSimilarity(String a, String b) {
        var wordsA = new java.util.HashSet<>(List.of(a.split("\\s+")));
        var wordsB = new java.util.HashSet<>(List.of(b.split("\\s+")));

        var intersection = new java.util.HashSet<>(wordsA);
        intersection.retainAll(wordsB);

        var union = new java.util.HashSet<>(wordsA);
        union.addAll(wordsB);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}