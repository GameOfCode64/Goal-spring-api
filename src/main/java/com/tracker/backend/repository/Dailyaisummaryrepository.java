package com.tracker.backend.repository;

import com.tracker.backend.entity.Dailyaisummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Dailyaisummaryrepository extends JpaRepository<Dailyaisummary, Long> {
    Optional<Dailyaisummary> findByUserIdAndSummaryDate(Long userId, LocalDate summaryDate);

    // For the in-app history view (e.g. "last 30 days of coach reports").
    List<Dailyaisummary> findByUserIdOrderBySummaryDateDesc(Long userId);

}
