package com.tracker.backend.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "daily_goals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goal_date", "description"}),
        indexes = @Index(name = "idx_goals_user_date", columnList = "user_id, goal_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal_date", nullable = false)
    private LocalDate goalDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_minutes")
    private Integer targetMinutes;

    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

}
