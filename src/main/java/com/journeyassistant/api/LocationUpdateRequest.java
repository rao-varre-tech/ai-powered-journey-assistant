package com.journeyassistant.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;

public record LocationUpdateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,
        @NotNull @PastOrPresent
        Instant timestamp) {
}
