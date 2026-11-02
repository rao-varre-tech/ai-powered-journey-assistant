package com.journeyassistant.api;

import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyApplicationService.LocationUpdateResult;

import java.time.Instant;
import java.util.UUID;

public record LocationUpdateResponse(UUID journeyId, double distanceToDestinationMeters,
                                     JourneyState state, Instant timestamp, boolean arrivedNow) {

    static LocationUpdateResponse from(LocationUpdateResult result) {
        return new LocationUpdateResponse(result.journeyId(), result.distanceMeters(), result.state(),
                result.timestamp(), result.arrivedNow());
    }
}
