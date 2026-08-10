package com.tracker.backend.controller;

import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import com.tracker.backend.service.CoachReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TEMPORARY - manually triggers a coach report immediately instead of
 * waiting for the actual scheduled hour. Delete once the scheduler has
 * been observed running correctly for a few real days.
 */
@RestController
@RequiredArgsConstructor
public class CoachReportTestController {

    private final UserRepository userRepository;
    private final CoachReportService coachReportService;

    // Try: GET /test/coach-report?userId=1
    @GetMapping("/test/coach-report")
    public Map<String, String> triggerReport(@RequestParam Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown userId: " + userId));

        coachReportService.generateAndSendReport(user);

        return Map.of("status", "triggered", "userId", String.valueOf(userId), "email", user.getEmail());
    }
}