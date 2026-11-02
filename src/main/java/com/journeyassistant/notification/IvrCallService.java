package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyEvent;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyStateMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class IvrCallService {
    private final JourneyRepository journeys;
    private final NotificationAttemptRepository attempts;
    private final IvrClient client;
    private final JourneyStateMachine states;
    private final int maxAttempts;

    public IvrCallService(JourneyRepository journeys, NotificationAttemptRepository attempts, IvrClient client,
                          JourneyStateMachine states, @Value("${ivr.max-attempts:3}") int maxAttempts) {
        this.journeys = journeys; this.attempts = attempts; this.client = client; this.states = states;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public String placeCall(UUID journeyId) {
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (journey.getState() == JourneyState.DECLINED || journey.getState() == JourneyState.ACCEPTED) return null;
        long count = attempts.countByJourney_Id(journeyId);
        if (count >= maxAttempts) return null;
        if (journey.getState() == JourneyState.RETRY_SCHEDULED) journey.applyEvent(JourneyEvent.RETRY_DUE, states);
        if (journey.getState() == JourneyState.ARRIVED) journey.applyEvent(JourneyEvent.CALL_STARTED, states);
        if (journey.getState() != JourneyState.CALLING) return null;
        NotificationAttempt attempt = attempts.save(new NotificationAttempt(journey, (int) count + 1, Instant.now()));
        String callSid = client.placeCall(journeyId, journey.getTravellerPhone());
        attempt.assignProviderCallId(callSid);
        return callSid;
    }
}
