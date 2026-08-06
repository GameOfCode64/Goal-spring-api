package com.tracker.backend.entity;

/**
 * Classification taxonomy applied to every tracked activity interval.
 * Matches the CHECK constraint on classification_cache.tag and
 * timeline_activities.tag in the Postgres schema.
 */


public enum Tag {
    STUDY,
    WORK,
    ENTERTAINMENT,
    WASTE,
    NEUTRAL
}
