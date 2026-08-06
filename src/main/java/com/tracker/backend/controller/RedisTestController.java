package com.tracker.backend.controller;

import com.tracker.backend.service.RedisUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

/**
 * TEMPORARY - for manually verifying RedisUsageService against the real
 * Upstash instance during development. Delete this class once the real
 * tracking-ingest endpoint exists and exercises the same code paths.
 */

@RestController
@RequiredArgsConstructor
public class RedisTestController {
    private final RedisUsageService redisUsageService;


    // Try: GET /test/redis/usage?userId=1&category=ENTERTAINMENT&seconds=300
    @GetMapping("/test/redis/usage")
    public Map<String, Object> testUsageIncrement(
            @RequestParam Long userId,
            @RequestParam String category,
            @RequestParam long seconds
    ) {
        ZoneId zone = ZoneId.of("Asia/Kolkata"); // hardcoded for this manual test only
        long newTotal = redisUsageService.incrementCategoryUsage(userId, category, seconds, zone);
        long confirmedTotal = redisUsageService.getCategoryUsageSeconds(userId, category, zone);

        return Map.of(
                "userId", userId,
                "category", category,
                "secondsAdded", seconds,
                "newTotalFromIncrement", newTotal,
                "confirmedTotalFromRead", confirmedTotal
        );
    }

    // Try: GET /test/redis/leaderboard?userId=1&minutes=25
    @GetMapping("/test/redis/leaderboard")
    public Map<String, Object> testLeaderboard(
            @RequestParam Long userId,
            @RequestParam double minutes
    ) {
        redisUsageService.incrementLeaderboardScore(userId, minutes);
        Set<String> top10 = redisUsageService.getTopLeaderboard(10);
        Long rank = redisUsageService.getUserRank(userId);

        return Map.of(
                "userId", userId,
                "minutesAdded", minutes,
                "top10", top10,
                "yourRank", rank != null ? rank : "no score yet"
        );
    }

    // Try: GET /test/redis/alert?userId=1&category=ENTERTAINMENT&tier=WARN_80
    // Call it twice with the same params - first call should return
    // shouldFire=true, second call should return shouldFire=false (dedup working).
    @GetMapping("/test/redis/alert")
    public Map<String, Object> testAlertDedup(
            @RequestParam Long userId,
            @RequestParam String category,
            @RequestParam String tier
    ) {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        boolean shouldFire = redisUsageService.shouldFireBudgetAlert(userId, category, tier, zone);

        return Map.of(
                "userId", userId,
                "category", category,
                "tier", tier,
                "shouldFire", shouldFire
        );
    }
}
