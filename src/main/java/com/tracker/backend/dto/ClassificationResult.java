package com.tracker.backend.dto;

import com.tracker.backend.entity.Tag;

public record ClassificationResult(
        Tag tag,
        String source,
        boolean wasNewClassification
) {

}
