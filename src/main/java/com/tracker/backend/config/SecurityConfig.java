package com.tracker.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY - permits all requests with no authentication.
 *
 * This exists only so local development/testing isn't blocked while the
 * real Google OAuth2 + JWT security layer hasn't been built yet. This
 * MUST be replaced before any real user data is exposed - a permitAll()
 * config like this in production would let anyone read/write anyone
 * else's tracking data.
 *
 * TODO: replace with real OAuth2 login + JWT filter chain.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable()) // fine for a stateless API under active dev; revisit for prod
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}