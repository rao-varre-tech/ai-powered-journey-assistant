package com.journeyassistant.api;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyState;

import java.time.Instant;
import java.util.UUID;

public record CreateJourneyResponse(UUID journeyId, JourneyState state, Instant createdAt) {

    static CreateJourneyResponse from(Journey journey) {
        return new CreateJourneyResponse(journey.getId(), journey.getState(), journey.getCreatedAt());
    }
}
