package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import com.tracker.backend.entity.DeviceType;
import com.tracker.backend.entity.Tag;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "timeline_activities",
        indexes = {
                @Index(name = "idx_timeline_user_date", columnList = "user_id, local_date"),
                @Index(name = "idx_timeline_user_tag", columnList = "user_id, tag, local_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "app_name", nullable = false, length = 120)
    private String appName;

    @Column(name = "window_title", columnDefinition = "TEXT")
    private String windowTitle;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tag tag;


    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private OffsetDateTime endedAt;


    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    // Pre-computed in the user's own timezone at write time, so day-boundary
    // queries never need to re-derive "today" from a UTC timestamp.
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;
}
