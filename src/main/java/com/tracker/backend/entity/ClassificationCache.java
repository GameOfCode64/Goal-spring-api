package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Tier 1 of the classification funnel. A lookup here (by pattern_hash) that
 * hits means the tick is tagged instantly with zero Gemini calls. Every
 * Gemini classification result (Tier 3) gets written back here so the
 * system never re-asks about the same title twice.
 */

@Entity
@Table(name="classification_cache")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_text", nullable = false, columnDefinition = "TEXT")
    private String patternText;

    // sha256 of the normalized pattern_text - what we actually index/query on
    @Column(name = "pattern_hash", nullable = false, unique = true, length = 64)
    private String patternHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tag tag;

    @Column(name= "app_name", length = 120)
    private String appName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CacheSource source = CacheSource.AI;

    @Column(precision = 3, scale = 2)
    private BigDecimal confidence;

    @Builder.Default
    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 1;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt = OffsetDateTime.now();

}
