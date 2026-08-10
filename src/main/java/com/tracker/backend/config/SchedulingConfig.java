package com.tracker.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled methods (CoachReportScheduler) to actually run.
 * Without this, @Scheduled annotations are silently ignored.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}