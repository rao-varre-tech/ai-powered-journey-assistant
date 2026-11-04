package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyEvent;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyStateMachine;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrySchedulingTest {
    @Test void pressingTwoSchedulesRetryFiveMinutesLater() {
        UUID id = UUID.randomUUID();
        Journey journey = mock(Journey.class);
        NotificationAttempt attempt = mock(NotificationAttempt.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        JourneyRepository journeys = mock(JourneyRepository.class);
        RetryScheduler scheduler = mock(RetryScheduler.class);
        when(attempts.findByProviderCallIdForUpdate("CA1")).thenReturn(Optional.of(attempt));
        when(attempts.findByProviderCallId("CA1")).thenReturn(Optional.of(attempt));
        when(attempts.findTopByJourney_IdOrderByAttemptNumberDesc(id)).thenReturn(Optional.of(attempt));
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(attempt.getJourney()).thenReturn(journey);
        when(attempt.getAttemptNumber()).thenReturn(1);
        when(attempt.getJourney().getId()).thenReturn(id);
        when(journey.getState()).thenReturn(JourneyState.CALLING);
        when(attempt.getInvalidDigitCount()).thenReturn(0);
        IvrWebhookService service = new IvrWebhookService(journeys, attempts, new JourneyStateMachine(), new IvrTwimlFactory("https://journey.example.test"),
                scheduler, mock(NotificationService.class), 3);

        String twiml = service.processDigit(id, "CA1", "2");

        assertTrue(twiml.contains("call you again in five minutes"));
        verify(journey).applyEvent(eq(JourneyEvent.RETRY), any(JourneyStateMachine.class));
        verify(scheduler).scheduleRetry(id, 1, Duration.ofMinutes(5));
    }

    @Test void staleScheduledTaskDoesNotCallAgain() {
        UUID id = UUID.randomUUID();
        JourneyRepository journeys = mock(JourneyRepository.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        IvrClient client = mock(IvrClient.class);
        Journey journey = mock(Journey.class);
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(journey.getState()).thenReturn(JourneyState.ACCEPTED);
        IvrCallService calls = new IvrCallService(journeys, attempts, client, new JourneyStateMachine(), 3);

        assertNull(calls.runScheduledRetry(id));
        verify(attempts, never()).countByJourney_Id(id);
        verify(client, never()).placeCall(any(), any());
    }

    @Test void scheduledRetryPlacesCallOnlyWhenStillScheduledAndUnderLimit() {
        UUID id = UUID.randomUUID();
        JourneyRepository journeys = mock(JourneyRepository.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        IvrClient client = mock(IvrClient.class);
        Journey journey = mock(Journey.class);
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(journey.getState()).thenReturn(JourneyState.RETRY_SCHEDULED);
        when(journey.hasNotificationConsent()).thenReturn(true);
        when(journey.getTravellerPhone()).thenReturn("+15550001111");
        when(attempts.countByJourney_Id(id)).thenReturn(1L);
        when(attempts.save(any(NotificationAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.placeCall(id, "+15550001111")).thenReturn("CA2");
        IvrCallService calls = new IvrCallService(journeys, attempts, client, new JourneyStateMachine(), 3);

        assertTrue("CA2".equals(calls.runScheduledRetry(id)));
        verify(journey).applyEvent(eq(JourneyEvent.RETRY_DUE), any(JourneyStateMachine.class));
        verify(client).placeCall(id, "+15550001111");
    }

    @Test void exhaustedJourneyTransitionsToNoResponseWithoutCalling() {
        UUID id = UUID.randomUUID();
        JourneyRepository journeys = mock(JourneyRepository.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        IvrClient client = mock(IvrClient.class);
        Journey journey = mock(Journey.class);
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(journey.getState()).thenReturn(JourneyState.RETRY_SCHEDULED);
        when(journey.hasNotificationConsent()).thenReturn(true);
        when(attempts.countByJourney_Id(id)).thenReturn(4L);
        IvrCallService calls = new IvrCallService(journeys, attempts, client, new JourneyStateMachine(), 3);

        assertNull(calls.runScheduledRetry(id));
        verify(journey).applyEvent(eq(JourneyEvent.RETRIES_EXHAUSTED), any(JourneyStateMachine.class));
        verify(client, never()).placeCall(any(), any());
    }
}
