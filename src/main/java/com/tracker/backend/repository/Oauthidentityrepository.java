package com.tracker.backend.repository;

import com.tracker.backend.entity.Oauthidentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Oauthidentityrepository extends JpaRepository<Oauthidentity, Long> {
    // The lookup used on every Google login: "have we seen this Google
    // account before?" — if yes, log them in; if no, create a new User.
    Optional<Oauthidentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
