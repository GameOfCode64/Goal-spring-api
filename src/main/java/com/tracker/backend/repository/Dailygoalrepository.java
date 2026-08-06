package com.tracker.backend.repository;

import com.tracker.backend.entity.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface Dailygoalrepository extends JpaRepository<DailyGoal, Long> {
    // Powers both: (a) today's goal list shown in-app, and
    // (b) the "goal compliance" section of the nightly coach prompt.
    List<DailyGoal> findByUserIdAndGoalDate(Long userId, LocalDate goalDate);

    // Used by the "Tomorrow's Goal Banner" feature — checks whether the
    // user has already set anything for tomorrow before showing the
    // sticky reminder card.
    boolean existsByUserIdAndGoalDate(Long userId, LocalDate goalDate);
}
