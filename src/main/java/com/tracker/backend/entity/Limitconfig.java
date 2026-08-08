package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import com.tracker.backend.entity.ScopeType;


/**
 * A user's configured daily budget, e.g. "ENTERTAINMENT = 300 minutes"
 * (scopeType=CATEGORY) or "Instagram = 90 minutes" (scopeType=APP, nested
 * inside a broader category budget). Read once per user and cached in the
 * service layer / Redis alongside the usage hash rather than re-queried on
 * every tick.
 */

@Entity
@Table(
        name = "limit_config",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "scope_type", "scope_value"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Limitconfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 10)
    private ScopeType scopeType;

    // e.g. "ENTERTAINMENT" when scopeType=CATEGORY, or "Instagram" when scopeType=APP
    @Column(name = "scope_value", nullable = false, length = 60)
    private String scopeValue;

    @Column(name = "daily_limit_minutes", nullable = false)
    private Integer dailyLimitMinutes;

    @Builder.Default
    @Column(name = "warn_threshold_pct", nullable = false)
    private Integer warnThresholdPct = 80;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
