package com.journeyassistant.api;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyState;

import java.time.Instant;
import java.util.UUID;

public record JourneyStatusResponse(UUID journeyId, JourneyState state, Instant arrivedAt,
                                    Instant createdAt, Instant updatedAt) {

    static JourneyStatusResponse from(Journey journey) {
        return new JourneyStatusResponse(journey.getId(), journey.getState(), journey.getArrivedAt(),
                journey.getCreatedAt(), journey.getUpdatedAt());
    }
}
