package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One row per user per day, storing both the raw Gemini JSON response
 * (audit trail / debugging) and the rendered HTML actually emailed via
 * Resend. Also doubles as the data source for an in-app "history" view.
 */


@Entity
@Table(
        name = "daily_ai_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "summary_date"}),
        indexes = @Index(name = "idx_summaries_user_date", columnList = "user_id, summary_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dailyaisummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "raw_gemini_response", nullable = false, columnDefinition = "TEXT")
    private String rawGeminiResponse;

    @Column(name = "html_rendered", nullable = false, columnDefinition = "TEXT")
    private String htmlRendered;

    @Column(name = "email_sent_at")
    private OffsetDateTime emailSentAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
