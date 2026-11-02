package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyStateMachine;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentGateTest {
    @Test void noIvrCallIsPlacedWithoutConsent() {
        UUID id = UUID.randomUUID();
        JourneyRepository journeys = mock(JourneyRepository.class);
        NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
        IvrClient client = mock(IvrClient.class);
        Journey journey = mock(Journey.class);
        when(journeys.findByIdForUpdate(id)).thenReturn(Optional.of(journey));
        when(journey.hasNotificationConsent()).thenReturn(false);
        when(journey.getState()).thenReturn(JourneyState.ARRIVED);
        IvrCallService service = new IvrCallService(journeys, attempts, client, new JourneyStateMachine(), 3);

        assertNull(service.placeCall(id));
        verify(attempts, never()).countByJourney_Id(id);
        verify(client, never()).placeCall(id, null);
    }
}
