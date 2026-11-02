package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyEvent;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import com.journeyassistant.journey.JourneyStateMachine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class IvrWebhookService {
    private final JourneyRepository journeys;
    private final NotificationAttemptRepository attempts;
    private final JourneyStateMachine states;
    private final IvrTwimlFactory twiml;
    private final int maxAttempts;

    public IvrWebhookService(JourneyRepository journeys, NotificationAttemptRepository attempts,
                             JourneyStateMachine states, IvrTwimlFactory twiml,
                             @Value("${ivr.max-attempts:3}") int maxAttempts) {
        this.journeys = journeys; this.attempts = attempts; this.states = states; this.twiml = twiml;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public String processDigit(UUID journeyId, String callSid, String digit) {
        NotificationAttempt attempt = attempts.findByProviderCallIdForUpdate(callSid).orElseThrow();
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (!attempt.getJourney().getId().equals(journeyId)) throw new IllegalArgumentException("Call does not belong to journey");
        if (digit == null || digit.length() != 1) digit = "";
        if (digit.equals(attempt.getLastProcessedDigit()))
            return twiml.responseForDigit(digit, journeyId, attempt.getInvalidDigitCount());
        attempt.recordDigit(digit);
        switch (digit) {
            case "1" -> { if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) journey.applyEvent(JourneyEvent.ACCEPT, states); }
            case "2" -> { if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) journey.applyEvent(JourneyEvent.RETRY, states); }
            case "3" -> { if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) journey.applyEvent(JourneyEvent.REJECT, states); }
            default -> { }
        }
        return twiml.responseForDigit(digit, journeyId, attempt.getInvalidDigitCount());
    }

    @Transactional
    public void processStatus(String callSid, String callStatus) {
        NotificationAttempt attempt = attempts.findByProviderCallIdForUpdate(callSid).orElseThrow();
        NotificationAttemptStatus mapped = switch (callStatus == null ? "" : callStatus.toLowerCase()) {
            case "completed" -> NotificationAttemptStatus.COMPLETED;
            case "no-answer", "busy", "failed", "canceled" -> NotificationAttemptStatus.FAILED;
            case "in-progress", "ringing", "queued", "initiated" -> NotificationAttemptStatus.IN_PROGRESS;
            default -> attempt.getStatus();
        };
        attempt.updateStatus(mapped);
        if (mapped == NotificationAttemptStatus.FAILED) {
            Journey journey = journeys.findByIdForUpdate(attempt.getJourney().getId()).orElseThrow();
            if (!isFinal(journey.getState()) && attempt.getAttemptNumber() < maxAttempts
                    && journey.getState() == JourneyState.CALLING) journey.applyEvent(JourneyEvent.RETRY, states);
        }
    }

    private boolean isFinal(JourneyState state) { return state == JourneyState.ACCEPTED || state == JourneyState.DECLINED; }
}
