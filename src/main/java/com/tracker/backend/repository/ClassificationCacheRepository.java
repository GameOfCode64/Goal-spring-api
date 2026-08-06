package com.tracker.backend.repository;

import com.tracker.backend.entity.ClassificationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassificationCacheRepository extends JpaRepository<ClassificationCache, Long> {

    // Tier 1 of the classification funnel — this is called on EVERY
    // incoming tick before anything else, so patternHash is indexed
    // (unique constraint) specifically to keep this fast.
    Optional<ClassificationCache> findByPatternHash(String patternHash);

    // Bumps hit_count and last_seen_at whenever a cache hit occurs, without
    // needing to load-then-save the whole entity. Kept as a single UPDATE
    // to avoid unnecessary round trips on a path that runs very frequently.
    @Modifying
    @Query("""
        UPDATE ClassificationCache c
        SET c.hitCount = c.hitCount + 1, c.lastSeenAt = CURRENT_TIMESTAMP
        WHERE c.patternHash = :patternHash
        """)
    void recordHit(@Param("patternHash") String patternHash);
}