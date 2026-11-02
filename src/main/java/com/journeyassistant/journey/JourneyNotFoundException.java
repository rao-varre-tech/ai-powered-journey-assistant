package com.journeyassistant.journey;

import java.util.UUID;

public final class JourneyNotFoundException extends RuntimeException {

    public JourneyNotFoundException(UUID journeyId) {
        super("Journey not found: " + journeyId);
    }
}
