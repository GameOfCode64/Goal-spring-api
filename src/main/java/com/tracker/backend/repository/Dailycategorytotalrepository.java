package com.tracker.backend.repository;

import com.tracker.backend.entity.Dailycategorytotal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface Dailycategorytotalrepository extends JpaRepository<Dailycategorytotal, Long> {
    List<Dailycategorytotal> findByUserIdAndLocalDate(Long userId, LocalDate localDate);

    // Used by the midnight rollover job to check whether today's row
    // already exists before deciding insert vs. update (upsert pattern).
    Optional<Dailycategorytotal> findByUserIdAndLocalDateAndCategory(
            Long userId, LocalDate localDate, String category
    );
}
