package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyStateMachine;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IvrWebhookServiceTest {
    @Test void duplicateDigitDeliveryDoesNotProcessDigitAgain() {
        UUID id = UUID.randomUUID();
        Journey journey = mock(Journey.class);
        NotificationAttempt attempt = mock(NotificationAttempt.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        JourneyRepository journeys = mock(JourneyRepository.class);
        when(attempts.findByProviderCallIdForUpdate("CA123")).thenReturn(Optional.of(attempt));
        when(attempts.findByProviderCallId("CA123")).thenReturn(Optional.of(attempt));
        when(attempts.findTopByJourney_IdOrderByAttemptNumberDesc(id)).thenReturn(Optional.of(attempt));
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(attempt.getJourney()).thenReturn(journey);
        when(journey.getId()).thenReturn(id);
        when(attempt.getLastProcessedDigit()).thenReturn("1");
        when(attempt.getInvalidDigitCount()).thenReturn(0);
        IvrWebhookService service = new IvrWebhookService(journeys, attempts, new JourneyStateMachine(), new IvrTwimlFactory("https://journey.example.test"),
                mock(RetryScheduler.class), mock(NotificationService.class), 3);

        String xml = service.processDigit(id, "CA123", "1");

        assertTrue(xml.contains("ride link will be sent shortly"));
        verify(attempt, never()).recordDigit("1");
        verify(journey, never()).applyEvent(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test void staleDigitFromPreviousCallCannotActOnTheRetryCall() {
        UUID id = UUID.randomUUID();
        Journey journey = mock(Journey.class);
        NotificationAttempt oldAttempt = mock(NotificationAttempt.class);
        NotificationAttempt latestAttempt = mock(NotificationAttempt.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        JourneyRepository journeys = mock(JourneyRepository.class);
        when(attempts.findByProviderCallId("CALL-OLD")).thenReturn(Optional.of(oldAttempt));
        when(attempts.findByProviderCallIdForUpdate("CALL-OLD")).thenReturn(Optional.of(oldAttempt));
        when(attempts.findTopByJourney_IdOrderByAttemptNumberDesc(id)).thenReturn(Optional.of(latestAttempt));
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(oldAttempt.getJourney()).thenReturn(journey);
        when(oldAttempt.getId()).thenReturn(UUID.randomUUID());
        when(oldAttempt.getInvalidDigitCount()).thenReturn(0);
        when(latestAttempt.getId()).thenReturn(UUID.randomUUID());
        when(journey.getId()).thenReturn(id);
        RetryScheduler scheduler = mock(RetryScheduler.class);
        IvrWebhookService service = new IvrWebhookService(journeys, attempts, new JourneyStateMachine(),
                new IvrTwimlFactory("https://journey.example.test"), scheduler, mock(NotificationService.class), 3);

        service.processDigit(id, "CALL-OLD", "2");

        verify(oldAttempt, never()).recordDigit(any());
        verify(journey, never()).applyEvent(any(), any());
        verify(scheduler, never()).scheduleRetry(any(), anyInt(), any());
    }
}
