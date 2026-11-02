package com.journeyassistant.journey;

import com.journeyassistant.location.HaversineDistance;
import com.journeyassistant.location.LocationUpdate;
import com.journeyassistant.location.LocationUpdateRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JourneyApplicationService {

    private final JourneyRepository journeyRepository;
    private final LocationUpdateRepository locationUpdateRepository;
    private final JourneyStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;

    public JourneyApplicationService(JourneyRepository journeyRepository,
                                     LocationUpdateRepository locationUpdateRepository,
                                     JourneyStateMachine stateMachine,
                                     ApplicationEventPublisher eventPublisher) {
        this.journeyRepository = journeyRepository;
        this.locationUpdateRepository = locationUpdateRepository;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Journey createJourney(String userPhone, double destinationLatitude,
                                 double destinationLongitude, int geofenceRadiusMeters) {
        return journeyRepository.save(new Journey(
                userPhone, destinationLatitude, destinationLongitude, geofenceRadiusMeters));
    }

    @Transactional(readOnly = true)
    public Journey getJourney(UUID journeyId) {
        return journeyRepository.findById(journeyId)
                .orElseThrow(() -> new JourneyNotFoundException(journeyId));
    }

    @Transactional
    public LocationUpdateResult updateLocation(UUID journeyId, double latitude, double longitude,
                                               Instant timestamp) {
        Journey journey = journeyRepository.findByIdForUpdate(journeyId)
                .orElseThrow(() -> new JourneyNotFoundException(journeyId));

        double distanceMeters = HaversineDistance.betweenMeters(
                latitude, longitude,
                journey.getDestinationLatitude(), journey.getDestinationLongitude());
        locationUpdateRepository.save(new LocationUpdate(journey, latitude, longitude, timestamp));

        boolean arrivedNow = journey.getState() == JourneyState.TRACKING
                && distanceMeters <= journey.getGeofenceRadiusMeters();
        if (arrivedNow) {
            journey.applyEvent(JourneyEvent.DESTINATION_REACHED, stateMachine);
            journey.markArrivedAt(timestamp);
            journeyRepository.save(journey);
            eventPublisher.publishEvent(new JourneyArrivedEvent(journey.getId(), timestamp, distanceMeters));
        }

        return new LocationUpdateResult(journey.getId(), distanceMeters, journey.getState(), timestamp, arrivedNow);
    }

    public record LocationUpdateResult(UUID journeyId, double distanceMeters, JourneyState state,
                                       Instant timestamp, boolean arrivedNow) {
    }
}
