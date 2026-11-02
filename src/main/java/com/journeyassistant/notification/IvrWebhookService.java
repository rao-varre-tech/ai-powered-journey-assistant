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
import java.time.Duration;
import java.util.Objects;

@Service
public class IvrWebhookService {
    private final JourneyRepository journeys;
    private final NotificationAttemptRepository attempts;
    private final JourneyStateMachine states;
    private final IvrTwimlFactory twiml;
    private final RetryScheduler retryScheduler;
    private final NotificationService notificationService;
    private final int maxRetries;

    public IvrWebhookService(JourneyRepository journeys, NotificationAttemptRepository attempts,
                             JourneyStateMachine states, IvrTwimlFactory twiml, RetryScheduler retryScheduler,
                             NotificationService notificationService,
                             @Value("${ivr.max-retries:3}") int maxRetries) {
        this.journeys = journeys; this.attempts = attempts; this.states = states; this.twiml = twiml;
        this.retryScheduler = retryScheduler;
        this.notificationService = notificationService;
        this.maxRetries = maxRetries;
    }

    @Transactional
    public String processDigit(UUID journeyId, String callSid, String digit) {
        if (callSid == null || callSid.isBlank() || journeyId == null) throw new IllegalArgumentException("Journey and call ids are required");
        NotificationAttempt lookup = attempts.findByProviderCallId(callSid).orElseThrow();
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        NotificationAttempt attempt = attempts.findByProviderCallIdForUpdate(callSid).orElseThrow();
        if (!lookup.getJourney().getId().equals(journeyId)
                || !attempt.getJourney().getId().equals(journeyId)) throw new IllegalArgumentException("Call does not belong to journey");
        if (!isLatestAttempt(attempt)) return twiml.responseForDigit(digit == null ? "" : digit, journeyId, attempt.getInvalidDigitCount());
        if (digit == null || digit.length() != 1) digit = "";
        if (digit.equals(attempt.getLastProcessedDigit()))
            return twiml.responseForDigit(digit, journeyId, attempt.getInvalidDigitCount());
        attempt.recordDigit(digit);
        boolean retriesExhausted = false;
        switch (digit) {
            case "1" -> {
                if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) {
                    journey.applyEvent(JourneyEvent.ACCEPT, states);
                    notificationService.sendRideLink(journeyId);
                }
            }
            case "2" -> {
                if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) {
                    if (retriesUsed(attempt) >= maxRetries) {
                        journey.applyEvent(JourneyEvent.RETRIES_EXHAUSTED, states);
                        retriesExhausted = true;
                    } else {
                        journey.applyEvent(JourneyEvent.RETRY, states);
                        retryScheduler.scheduleRetry(journeyId, attempt.getAttemptNumber(), Duration.ofMinutes(5));
                    }
                }
            }
            case "3" -> { if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) journey.applyEvent(JourneyEvent.REJECT, states); }
            default -> { }
        }
        if (retriesExhausted) return twiml.noMoreRetries();
        return twiml.responseForDigit(digit, journeyId, attempt.getInvalidDigitCount());
    }

    @Transactional
    public void processStatus(String callSid, String callStatus) {
        if (callSid == null || callSid.isBlank()) throw new IllegalArgumentException("Call id is required");
        NotificationAttempt lookup = attempts.findByProviderCallId(callSid).orElseThrow();
        Journey journey = journeys.findByIdForUpdate(lookup.getJourney().getId()).orElseThrow();
        NotificationAttempt attempt = attempts.findByProviderCallIdForUpdate(callSid).orElseThrow();
        NotificationAttemptStatus mapped = switch (callStatus == null ? "" : callStatus.toLowerCase()) {
            case "completed" -> NotificationAttemptStatus.COMPLETED;
            case "no-answer", "busy", "failed", "canceled" -> NotificationAttemptStatus.FAILED;
            case "in-progress", "ringing", "queued", "initiated" -> NotificationAttemptStatus.IN_PROGRESS;
            default -> attempt.getStatus();
        };
        boolean statusChanged = attempt.updateStatus(mapped);
        if (statusChanged && mapped == NotificationAttemptStatus.FAILED && isLatestAttempt(attempt)) {
            if (!isFinal(journey.getState()) && journey.getState() == JourneyState.CALLING) {
                if (retriesUsed(attempt) >= maxRetries) {
                    journey.applyEvent(JourneyEvent.RETRIES_EXHAUSTED, states);
                } else {
                    journey.applyEvent(JourneyEvent.RETRY, states);
                    retryScheduler.scheduleRetry(journey.getId(), attempt.getAttemptNumber(), Duration.ofMinutes(5));
                }
            }
        }
    }

    private boolean isFinal(JourneyState state) { return state == JourneyState.ACCEPTED || state == JourneyState.DECLINED || state == JourneyState.NO_RESPONSE; }

    private int retriesUsed(NotificationAttempt attempt) { return Math.max(0, attempt.getAttemptNumber() - 1); }

    private boolean isLatestAttempt(NotificationAttempt attempt) {
        return attempts.findTopByJourney_IdOrderByAttemptNumberDesc(attempt.getJourney().getId())
                .map(latest -> Objects.equals(latest.getId(), attempt.getId())).orElse(false);
    }
}
