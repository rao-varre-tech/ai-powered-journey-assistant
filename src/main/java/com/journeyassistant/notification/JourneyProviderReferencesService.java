package com.journeyassistant.notification;

import com.journeyassistant.journey.JourneyNotFoundException;
import com.journeyassistant.journey.JourneyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JourneyProviderReferencesService {
    private final JourneyRepository journeys;
    private final NotificationAttemptRepository attempts;
    private final JourneyMessageRepository messages;

    public JourneyProviderReferencesService(JourneyRepository journeys,
                                            NotificationAttemptRepository attempts,
                                            JourneyMessageRepository messages) {
        this.journeys = journeys;
        this.attempts = attempts;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public JourneyProviderReferences getReferences(UUID journeyId) {
        if (!journeys.existsById(journeyId)) throw new JourneyNotFoundException(journeyId);

        String latestCallSid = attempts.findTopByJourney_IdOrderByAttemptNumberDesc(journeyId)
                .map(NotificationAttempt::getProviderCallId).orElse(null);
        String smsMessageSid = null;
        String whatsappMessageSid = null;
        for (JourneyMessage message : messages.findAllByJourney_Id(journeyId)) {
            if (message.getChannel() == MessageChannelType.SMS) smsMessageSid = message.getProviderMessageId();
            if (message.getChannel() == MessageChannelType.WHATSAPP) whatsappMessageSid = message.getProviderMessageId();
        }
        return new JourneyProviderReferences(journeyId, latestCallSid, smsMessageSid, whatsappMessageSid);
    }
}
