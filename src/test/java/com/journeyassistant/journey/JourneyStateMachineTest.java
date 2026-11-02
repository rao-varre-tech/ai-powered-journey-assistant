package com.journeyassistant.journey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JourneyStateMachineTest {

    private final JourneyStateMachine stateMachine = new JourneyStateMachine();

    @Test
    void permitsEveryDefinedTransition() {
        Map<TransitionCase, JourneyState> expected = Map.of(
                new TransitionCase(JourneyState.TRACKING, JourneyEvent.DESTINATION_REACHED), JourneyState.ARRIVED,
                new TransitionCase(JourneyState.ARRIVED, JourneyEvent.CALL_STARTED), JourneyState.CALLING,
                new TransitionCase(JourneyState.CALLING, JourneyEvent.ACCEPT), JourneyState.ACCEPTED,
                new TransitionCase(JourneyState.CALLING, JourneyEvent.RETRY), JourneyState.RETRY_SCHEDULED,
                new TransitionCase(JourneyState.CALLING, JourneyEvent.REJECT), JourneyState.DECLINED,
                new TransitionCase(JourneyState.RETRY_SCHEDULED, JourneyEvent.RETRY_DUE), JourneyState.CALLING,
                new TransitionCase(JourneyState.RETRY_SCHEDULED, JourneyEvent.RETRIES_EXHAUSTED), JourneyState.NO_RESPONSE,
                new TransitionCase(JourneyState.CALLING, JourneyEvent.RETRIES_EXHAUSTED), JourneyState.NO_RESPONSE
        );

        expected.forEach((transition, nextState) -> assertEquals(
                nextState, stateMachine.transition(transition.state(), transition.event()),
                () -> transition.state() + " + " + transition.event()));
    }

    @ParameterizedTest
    @EnumSource(JourneyState.class)
    void rejectsEveryUndefinedEventForEachState(JourneyState state) {
        for (JourneyEvent event : JourneyEvent.values()) {
            if (isValid(state, event)) {
                continue;
            }

            InvalidJourneyTransitionException exception = assertThrows(
                    InvalidJourneyTransitionException.class,
                    () -> stateMachine.transition(state, event),
                    () -> state + " + " + event);
            assertTrue(exception.getMessage().contains(state.name()));
            assertTrue(exception.getMessage().contains(event.name()));
        }
    }

    @Test
    void rejectsNullStateOrEvent() {
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(null, JourneyEvent.ACCEPT));
        assertThrows(IllegalArgumentException.class,
                () -> stateMachine.transition(JourneyState.CALLING, null));
    }

    private boolean isValid(JourneyState state, JourneyEvent event) {
        return switch (state) {
            case TRACKING -> event == JourneyEvent.DESTINATION_REACHED;
            case ARRIVED -> event == JourneyEvent.CALL_STARTED;
            case CALLING -> event == JourneyEvent.ACCEPT
                    || event == JourneyEvent.RETRY
                    || event == JourneyEvent.REJECT
                    || event == JourneyEvent.RETRIES_EXHAUSTED;
            case RETRY_SCHEDULED -> event == JourneyEvent.RETRY_DUE || event == JourneyEvent.RETRIES_EXHAUSTED;
            case ACCEPTED, DECLINED, NO_RESPONSE, REJECTED -> false;
        };
    }

    private record TransitionCase(JourneyState state, JourneyEvent event) {
    }
}
