package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_category_totals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "local_date", "category"}),
        indexes = @Index(name = "idx_daily_totals_user_date", columnList = "user_id, local_date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dailycategorytotal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    @Column(nullable = false, length = 20)
    private String category;

    @Builder.Default
    @Column(name = "total_seconds", nullable = false)
    private Integer totalSeconds = 0;

    @Builder.Default
    @Column(name = "limit_exceeded", nullable = false)
    private Boolean limitExceeded = false;

}
