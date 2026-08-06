package com.tracker.backend.repository;

import com.tracker.backend.entity.Tag;
import com.tracker.backend.entity.TimelineActivity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface Timelineactivityrepository extends JpaRepository<TimelineActivity, Long> {
    // Full day's timeline for a user - the raw input to the 9PM Gemini
    // coach prompt.
    List<TimelineActivity> findByUserIdAndLocalDateOrderByStartedAtAsc(Long userId, LocalDate localDate);

    // Sum of time spent per tag on a given day - used to build the
    // "Time by category" section of the coach prompt without pulling
    // every individual row into the app just to sum them.
    @Query("""
        SELECT t.tag AS tag, SUM(t.durationSeconds) AS totalSeconds
        FROM TimelineActivity t
        WHERE t.user.id = :userId AND t.localDate = :localDate
        GROUP BY t.tag
        """)
    List<TagDurationProjection> sumDurationByTagForDay(
            @Param("userId") Long userId,
            @Param("localDate") LocalDate localDate
    );

    // The single longest continuous WASTE/ENTERTAINMENT streak in a day,
    // needed for the "longest distraction streak" field in the coach
    // report. Simpler to compute this in the service layer from the
    // ordered timeline than as a single SQL query, so this repository
    // just exposes the ordered rows needed for that calculation.
    List<TimelineActivity> findByUserIdAndLocalDateAndTagOrderByStartedAtAsc(
            Long userId, LocalDate localDate, Tag tag
    );

    interface TagDurationProjection {
        Tag getTag();
        Long getTotalSeconds();
    }
}
