package com.tracker.backend.service;

import com.tracker.backend.dto.CoachReportResult;

import com.tracker.backend.entity.DailyGoal;
import com.tracker.backend.entity.Dailyaisummary;
import com.tracker.backend.entity.User;

import com.tracker.backend.repository.Dailyaisummaryrepository;
import com.tracker.backend.repository.Dailygoalrepository;
import com.tracker.backend.repository.TimelineActivityRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full nightly flow for ONE user:
 *   1. Pull the day's timeline aggregates + goal compliance from Postgres
 *   2. Format it into the prompt payload and call Gemini (CoachReportClient)
 *   3. Render the structured result into an HTML email
 *   4. Send via Resend (SMTP)
 *   5. Save the raw response + rendered HTML as a permanent audit row
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoachReportService {

    private final TimelineActivityRepository timelineActivityRepository;
    private final Dailygoalrepository dailyGoalRepository;
    private final Dailyaisummaryrepository dailyAiSummaryRepository;
    private final CoachReportClient coachReportClient;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from-address}")
    private String fromAddress;

    public void generateAndSendReport(User user) {
        ZoneId userZone = ZoneId.of(user.getTimezone());
        LocalDate today = LocalDate.now(userZone);

        String promptPayload = buildPromptPayload(user, today);

        CoachReportResult result;
        try {
            result = coachReportClient.generateReport(promptPayload);
        } catch (Exception e) {
            log.error("Coach report generation failed for user {}: {}", user.getId(), e.getMessage());
            return; // skip this user tonight rather than crash the whole scheduled job
        }

        String html = renderHtml(user, result);

        try {
            sendEmail(user.getEmail(), result.headline(), html);
        } catch (Exception e) {
            log.error("Failed to send coach email to user {}: {}", user.getId(), e.getMessage());
            // still save the summary below even if the email failed - the
            // report exists and can be viewed in-app / resent later.
        }

        saveSummary(user, today, result, html);
    }

    private String buildPromptPayload(User user, LocalDate today) {
        List<DailyGoal> goals = dailyGoalRepository.findByUserIdAndGoalDate(user.getId(), today);
        String goalsSummary = goals.isEmpty()
                ? "No goals set for today."
                : goals.stream()
                .map(g -> "- " + g.getDescription() + " [" + (g.getIsCompleted() ? "DONE" : "NOT DONE") + "]")
                .reduce("", (a, b) -> a + "\n" + b);

        var tagTotals = timelineActivityRepository.sumDurationByTagForDay(user.getId(), today);
        String timeByCategory = tagTotals.isEmpty()
                ? "No activity recorded today."
                : tagTotals.stream()
                .map(t -> t.getTag() + ": " + (t.getTotalSeconds() / 60) + " min")
                .reduce("", (a, b) -> a + "\n" + b);

        return """
            Date: %s
            Goals set for today:
            %s

            Time by category:
            %s
            """.formatted(today, goalsSummary, timeByCategory);
    }

    private String renderHtml(User user, CoachReportResult result) {
        String winsHtml = result.wins().stream()
                .map(w -> "<li>" + escapeHtml(w) + "</li>")
                .reduce("", (a, b) -> a + b);

        String leaksHtml = result.leaks().stream()
                .map(l -> "<li>" + escapeHtml(l) + "</li>")
                .reduce("", (a, b) -> a + b);

        return """
            <html>
            <body style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
                <h2>%s</h2>
                <p><strong>Verdict:</strong> %s</p>

                <h3>Wins</h3>
                <ul>%s</ul>

                <h3>Leaks</h3>
                <ul>%s</ul>

                <h3>Goal Compliance</h3>
                <p>%s</p>

                <h3>Tomorrow's Focus</h3>
                <p>%s</p>
            </body>
            </html>
            """.formatted(
                escapeHtml(result.headline()),
                escapeHtml(result.verdict()),
                winsHtml,
                leaksHtml,
                escapeHtml(result.goalComplianceNote()),
                escapeHtml(result.tomorrowFocus())
        );
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void sendEmail(String toAddress, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(toAddress);
        helper.setSubject("Your Daily Report: " + subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private void saveSummary(User user, LocalDate today, CoachReportResult result, String html) {
        try {
            Dailyaisummary summary = Dailyaisummary.builder()
                    .user(user)
                    .summaryDate(today)
                    .rawGeminiResponse(objectMapper.writeValueAsString(result))
                    .htmlRendered(html)
                    .emailSentAt(OffsetDateTime.now())
                    .build();
            dailyAiSummaryRepository.save(summary);
        } catch (Exception e) {
            log.error("Failed to save daily summary for user {}: {}", user.getId(), e.getMessage());
        }
    }
}