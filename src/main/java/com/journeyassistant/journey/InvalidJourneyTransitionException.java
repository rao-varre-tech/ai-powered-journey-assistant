package com.journeyassistant.journey;

public final class InvalidJourneyTransitionException extends IllegalStateException {

    public InvalidJourneyTransitionException(JourneyState state, JourneyEvent event) {
        super("Event " + event + " is not valid while a journey is in state " + state);
    }
}
