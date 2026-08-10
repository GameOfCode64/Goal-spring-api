package com.tracker.backend.scheduler;

import com.tracker.backend.entity.User;
import com.tracker.backend.repository.UserRepository;
import com.tracker.backend.service.CoachReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Runs every hour, on the hour. For each user, checks whether it's
 * currently their configured "coach report hour" (default 9PM) in THEIR
 * OWN timezone - not server time. This is the cross-cutting timezone
 * decision from the blueprint: a global cron tick would send reports at
 * the wrong local hour for anyone outside the server's timezone.
 *
 * Hourly granularity means a user gets their report within the same hour
 * as their configured time, not necessarily the exact minute - acceptable
 * for a once-daily digest.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoachReportScheduler {

    private final UserRepository userRepository;
    private final CoachReportService coachReportService;

    @Value("${app.scheduler.coach-report-hour:21}")
    private int coachReportHour;

    @Scheduled(cron = "0 0 * * * *") // top of every hour, server time
    public void runHourlyCheck() {
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            try {
                ZoneId userZone = ZoneId.of(user.getTimezone());
                int userLocalHour = LocalTime.now(userZone).getHour();

                if (userLocalHour == coachReportHour) {
                    log.info("Generating coach report for user {} (local hour {})", user.getId(), userLocalHour);
                    coachReportService.generateAndSendReport(user);
                }
            } catch (Exception e) {
                // one user's failure (bad timezone string, Gemini error, etc.)
                // must never stop the rest of the batch from processing.
                log.error("Failed processing coach report for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}