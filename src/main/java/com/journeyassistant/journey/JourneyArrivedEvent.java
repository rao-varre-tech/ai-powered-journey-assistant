package com.journeyassistant.journey;

import java.time.Instant;
import java.util.UUID;

public record JourneyArrivedEvent(UUID journeyId, Instant arrivedAt, double distanceMeters) {
}
