package com.journeyassistant.journey;

import com.journeyassistant.location.LocationUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JourneyApplicationServiceTest {

    @Mock
    private JourneyRepository journeyRepository;
    @Mock
    private LocationUpdateRepository locationUpdateRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JourneyApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JourneyApplicationService(journeyRepository, locationUpdateRepository,
                new JourneyStateMachine(), eventPublisher);
    }

    @Test
    void movesTrackingJourneyToArrivedAndPublishesEventWhenWithinRadius() {
        UUID journeyId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-10-31T10:15:30Z");
        Journey journey = new Journey("+919876543210", 12.9716, 77.5946, 250);
        when(journeyRepository.findByIdForUpdate(journeyId)).thenReturn(Optional.of(journey));

        JourneyApplicationService.LocationUpdateResult result = service.updateLocation(
                journeyId, 12.9716, 77.5946, timestamp);

        assertEquals(JourneyState.ARRIVED, journey.getState());
        assertEquals(timestamp, journey.getArrivedAt());
        assertTrue(result.arrivedNow());
        assertEquals(JourneyState.ARRIVED, result.state());
        verify(locationUpdateRepository).save(any());
        verify(journeyRepository).save(journey);
        verify(eventPublisher).publishEvent(any(JourneyArrivedEvent.class));
    }

    @Test
    void leavesJourneyTrackingWhenLocationIsOutsideRadius() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey("+919876543210", 12.9716, 77.5946, 100);
        when(journeyRepository.findByIdForUpdate(journeyId)).thenReturn(Optional.of(journey));

        JourneyApplicationService.LocationUpdateResult result = service.updateLocation(
                journeyId, 13.0, 77.6, Instant.parse("2026-10-31T10:15:30Z"));

        assertEquals(JourneyState.TRACKING, journey.getState());
        assertFalse(result.arrivedNow());
        assertTrue(result.distanceMeters() > journey.getGeofenceRadiusMeters());
        verify(locationUpdateRepository).save(any());
        verify(eventPublisher, never()).publishEvent(any(JourneyArrivedEvent.class));
    }

    @Test
    void doesNotPublishArrivalAgainAfterJourneyHasArrived() {
        UUID journeyId = UUID.randomUUID();
        Journey journey = new Journey("+919876543210", 12.9716, 77.5946, 250);
        journey.applyEvent(JourneyEvent.DESTINATION_REACHED, new JourneyStateMachine());
        when(journeyRepository.findByIdForUpdate(journeyId)).thenReturn(Optional.of(journey));

        JourneyApplicationService.LocationUpdateResult result = service.updateLocation(
                journeyId, 12.9716, 77.5946, Instant.parse("2026-10-31T10:15:30Z"));

        assertEquals(JourneyState.ARRIVED, result.state());
        assertFalse(result.arrivedNow());
        verify(eventPublisher, never()).publishEvent(any(JourneyArrivedEvent.class));
    }
}
