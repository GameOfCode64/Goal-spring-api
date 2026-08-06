package com.tracker.backend.repository;

import com.tracker.backend.entity.Limitconfig;
import com.tracker.backend.entity.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Limitconfigrepository extends JpaRepository<Limitconfig, Long> {
    // All active limits for a user - loaded once (e.g. on login or cached
    // in Redis alongside the usage hash) rather than queried on every tick.
    List<Limitconfig> findByUserIdAndIsActiveTrue(Long userId);

    Optional<Limitconfig> findByUserIdAndScopeTypeAndScopeValue(
            Long userId, ScopeType scopeType, String scopeValue
    );
}
