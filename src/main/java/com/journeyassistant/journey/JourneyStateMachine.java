package com.journeyassistant.journey;

import java.util.Map;

/** Pure domain logic for validating journey lifecycle transitions. */
public final class JourneyStateMachine {

    private static final Map<Transition, JourneyState> TRANSITIONS = Map.of(
            new Transition(JourneyState.TRACKING, JourneyEvent.DESTINATION_REACHED), JourneyState.ARRIVED,
            new Transition(JourneyState.ARRIVED, JourneyEvent.CALL_STARTED), JourneyState.CALLING,
            new Transition(JourneyState.CALLING, JourneyEvent.ACCEPT), JourneyState.ACCEPTED,
            new Transition(JourneyState.CALLING, JourneyEvent.RETRY), JourneyState.RETRY_SCHEDULED,
            new Transition(JourneyState.CALLING, JourneyEvent.REJECT), JourneyState.DECLINED,
            new Transition(JourneyState.RETRY_SCHEDULED, JourneyEvent.RETRY_DUE), JourneyState.CALLING
    );

    public JourneyState transition(JourneyState currentState, JourneyEvent event) {
        if (currentState == null || event == null) {
            throw new IllegalArgumentException("Current state and event must not be null");
        }

        JourneyState nextState = TRANSITIONS.get(new Transition(currentState, event));
        if (nextState == null) {
            throw new InvalidJourneyTransitionException(currentState, event);
        }
        return nextState;
    }

    private record Transition(JourneyState state, JourneyEvent event) {
    }
}
