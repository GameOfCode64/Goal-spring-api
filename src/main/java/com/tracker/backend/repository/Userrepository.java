package com.tracker.backend.repository;

import com.tracker.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface Userrepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
