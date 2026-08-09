package com.tracker.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "oauth_identities",
        uniqueConstraints =
        @UniqueConstraint(
                columnNames = {"provider", "provider_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String provider = "Google";

    @Column(name="provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "access_token_hash", length = 255)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
