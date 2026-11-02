package com.journeyassistant.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateJourneyRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "must be a valid international phone number")
        String userPhone,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double destinationLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double destinationLongitude,
        @NotNull @Positive @Max(100_000)
        Integer geofenceRadiusMeters,
        Boolean notificationConsent) {
}
