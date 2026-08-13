package com.tracker.backend.dto;

import com.tracker.backend.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceTokenRequest(
        @NotBlank String deviceToken,
        @NotNull Platform platform
) {
}