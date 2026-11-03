package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyEvent;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyStateMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class IvrCallService {
    private final JourneyRepository journeys;
    private final NotificationAttemptRepository attempts;
    private final IvrClient client;
    private final JourneyStateMachine states;
    private final int maxRetries;

    public IvrCallService(JourneyRepository journeys, NotificationAttemptRepository attempts, IvrClient client,
                          JourneyStateMachine states, @Value("${ivr.max-retries:3}") int maxRetries) {
        this.journeys = journeys; this.attempts = attempts; this.client = client; this.states = states;
        this.maxRetries = maxRetries;
    }

    // Arrival calls are launched by an AFTER_COMMIT listener. Use a fresh
    // transaction so the pessimistic journey lock is backed by an active tx.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String placeCall(UUID journeyId) {
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (!journey.hasNotificationConsent() || journey.getState() != JourneyState.ARRIVED) return null;
        long count = attempts.countByJourney_Id(journeyId);
        journey.applyEvent(JourneyEvent.CALL_STARTED, states);
        return placeCall(journey, journeyId, count);
    }

    @Transactional
    public String runScheduledRetry(UUID journeyId) {
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (!journey.hasNotificationConsent() || journey.getState() != JourneyState.RETRY_SCHEDULED) return null;
        long count = attempts.countByJourney_Id(journeyId);
        int retriesAlreadyPlaced = Math.max(0, (int) count - 1);
        if (retriesAlreadyPlaced >= maxRetries) {
            journey.applyEvent(JourneyEvent.RETRIES_EXHAUSTED, states);
            return null;
        }
        journey.applyEvent(JourneyEvent.RETRY_DUE, states);
        return placeCall(journey, journeyId, count);
    }

    private String placeCall(Journey journey, UUID journeyId, long count) {
        if (journey.getTravellerPhone() == null || journey.getTravellerPhone().isBlank()) {
            throw new IllegalStateException("Journey traveller phone is missing");
        }
        NotificationAttempt attempt = attempts.save(new NotificationAttempt(journey, (int) count + 1, Instant.now()));
        String callSid = client.placeCall(journeyId, journey.getTravellerPhone());
        if (callSid == null || callSid.isBlank()) {
            throw new IllegalStateException("IVR provider returned an empty call id");
        }
        attempt.assignProviderCallId(callSid);
        return callSid;
    }
}
