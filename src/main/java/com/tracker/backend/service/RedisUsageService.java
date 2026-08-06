package com.tracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;


/**
 * Owns every Redis key this app touches. Nothing else in the codebase
 * should build a Redis key string by hand — go through these methods so
 * the key format only lives in one place.
 *
 * Key formats (see blueprint section 3 for the full design rationale):
 *   session:{userId}                          HASH  - live streak state
 *   usage:{userId}:{yyyy-MM-dd}                HASH  - per-category seconds today
 *   alert:budget:{userId}:{category}:{date}:{tier}  STRING - dedup flag
 *   leaderboard:global:{yyyy-MM-dd}            ZSET  - study minutes today
 */


@Service
@RequiredArgsConstructor
public class RedisUsageService {
    private final RedisTemplate<String, String> redisTemplate;

//    public RedisUsageService(RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }

    public void setCurrentSession(Long userId, String tag, String appName, long startedAtEpochMs) {
        String key = sessionKey(userId);
        redisTemplate.opsForHash().put(key, "currentTag", tag);
        redisTemplate.opsForHash().put(key, "currentApp", appName);
        redisTemplate.opsForHash().put(key, "streakStartedAt", String.valueOf(startedAtEpochMs));
    }

    public String getCurrentTag(Long userId) {
        Object value = redisTemplate.opsForHash().get(sessionKey(userId), "currentTag");
        return value != null ? value.toString() : null;
    }

    public Long getStreakStartedAt(Long userId) {
        Object value = redisTemplate.opsForHash().get(sessionKey(userId), "streakStartedAt");
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    public void clearSession(Long userId) {
        redisTemplate.delete(sessionKey(userId));
    }

    private String sessionKey(Long userId) {
        return "session:" + userId;
    }

    // ============================================================
    // DAILY CATEGORY USAGE (budget tracking)
    // ============================================================

    /**
     * Atomically increments a category's running total for today and
     * returns the NEW total in seconds, so the caller can immediately
     * check it against the configured limit without a second round trip.
     */
    public long incrementCategoryUsage(Long userId, String category, long secondsToAdd, ZoneId userTimezone) {
        String key = usageKey(userId, userTimezone);
        Long newTotal = redisTemplate.opsForHash().increment(key, category, secondsToAdd);
        // Safety-net TTL in case the nightly Postgres rollup job ever lags -
        // the roll-up job is the real archival path, this just prevents
        // unbounded key growth if that job fails silently.
        redisTemplate.expire(key, Duration.ofHours(48));
        return newTotal != null ? newTotal : 0L;
    }

    public long getCategoryUsageSeconds(Long userId, String category, ZoneId userTimezone) {
        Object value = redisTemplate.opsForHash().get(usageKey(userId, userTimezone), category);
        return value != null ? Long.parseLong(value.toString()) : 0L;
    }

    private String usageKey(Long userId, ZoneId userTimezone) {
        LocalDate localDate = LocalDate.now(userTimezone);
        return "usage:" + userId + ":" + localDate;
    }

    // ============================================================
    // ALERT DE-DUPLICATION FLAGS
    // ============================================================

    /**
     * Returns true if this is the FIRST time this (user, category, tier)
     * has crossed its threshold today - i.e. whether the caller should
     * actually fire the alert. Sets the flag atomically so concurrent
     * requests can't both fire the same alert.
     *
     * tier examples: "WARN_80", "EXCEEDED_100"
     */
    public boolean shouldFireBudgetAlert(Long userId, String category, String tier, ZoneId userTimezone) {
        String key = alertFlagKey(userId, category, tier, userTimezone);
        // setIfAbsent = SET key value NX -> true only if the key didn't
        // already exist, which is exactly the "fire once" semantics we want.
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", untilMidnight(userTimezone));
        return Boolean.TRUE.equals(wasSet);
    }

    private String alertFlagKey(Long userId, String category, String tier, ZoneId userTimezone) {
        LocalDate localDate = LocalDate.now(userTimezone);
        return "alert:budget:" + userId + ":" + category + ":" + localDate + ":" + tier;
    }

    private Duration untilMidnight(ZoneId userTimezone) {
        LocalDateTime now = LocalDateTime.now(userTimezone);
        LocalDateTime midnight = LocalDate.now(userTimezone).plusDays(1).atStartOfDay();
        return Duration.between(now, midnight);
    }

    // ============================================================
    // REAL-TIME LEADERBOARD
    // ============================================================

    public void incrementLeaderboardScore(Long userId, double minutesToAdd) {
        redisTemplate.opsForZSet().incrementScore(leaderboardKey(), String.valueOf(userId), minutesToAdd);
    }

    /** Top N users by verified study minutes today, highest first. */
    public Set<String> getTopLeaderboard(int count) {
        return redisTemplate.opsForZSet().reverseRange(leaderboardKey(), 0, count - 1);
    }

    /** 0-based rank, or null if the user has no score yet today. */
    public Long getUserRank(Long userId) {
        return redisTemplate.opsForZSet().reverseRank(leaderboardKey(), String.valueOf(userId));
    }

    private String leaderboardKey() {
        // Server-clock date is fine here since the leaderboard is a
        // single global ranking, not a per-user-timezone concept.
        return "leaderboard:global:" + LocalDate.now();
    }
}
