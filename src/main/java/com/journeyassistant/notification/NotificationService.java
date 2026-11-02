package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.UUID;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final JourneyRepository journeys;
    private final JourneyMessageRepository messages;
    private final MessageChannel sms;
    private final MessageChannel whatsapp;
    private final RetryScheduler scheduler;
    private final NotificationStrategy strategy;
    private final Duration fallbackTimeout;
    private final String rideLink;

    public NotificationService(JourneyRepository journeys, JourneyMessageRepository messages,
            @Qualifier("smsChannel") MessageChannel sms, @Qualifier("whatsappChannel") MessageChannel whatsapp,
            RetryScheduler scheduler, @Value("${notification.strategy:sms}") String strategy,
            @Value("${notification.fallback-timeout:60s}") Duration fallbackTimeout,
            @Value("${notification.ride-link:}") String rideLink) {
        this.journeys = journeys;
        this.messages = messages;
        this.sms = sms;
        this.whatsapp = whatsapp;
        this.scheduler = scheduler;
        this.strategy = NotificationStrategy.parse(strategy);
        this.fallbackTimeout = fallbackTimeout;
        this.rideLink = rideLink;
    }

    @Transactional
    public void sendRideLink(UUID journeyId) {
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (!journey.hasNotificationConsent() || journey.getState() != JourneyState.ACCEPTED) return;
        if (rideLink == null || rideLink.isBlank()) {
            log.warn("Ride link notification skipped because RIDE_LINK is not configured for journey {}", journeyId);
            return;
        }
        String body = "Your ride link: " + rideLink + ". Open it to confirm your ride.";
        switch (strategy) {
            case SMS -> sendOnce(journey, MessageChannelType.SMS, sms, body);
            case WHATSAPP -> sendOnce(journey, MessageChannelType.WHATSAPP, whatsapp, body, Map.of("1", rideLink));
            case BOTH -> {
                sendOnce(journey, MessageChannelType.WHATSAPP, whatsapp, body, Map.of("1", rideLink));
                sendOnce(journey, MessageChannelType.SMS, sms, body);
            }
            case WHATSAPP_THEN_SMS -> {
                Dispatch dispatch = sendOnce(journey, MessageChannelType.WHATSAPP, whatsapp, body, Map.of("1", rideLink));
                if (dispatch.created()) {
                    if (dispatch.message().getStatus() == MessageDeliveryStatus.FAILED
                            || dispatch.message().getStatus() == MessageDeliveryStatus.UNDELIVERED) {
                        sendSmsFallback(journey, body);
                    } else {
                        scheduler.scheduleMessageFallback(journeyId, fallbackTimeout);
                    }
                }
            }
        }
    }

    @Transactional
    public void processStatus(String messageSid, String rawStatus) {
        JourneyMessage message = messages.findByProviderMessageIdForUpdate(messageSid).orElse(null);
        if (message == null) return;
        MessageDeliveryStatus status = MessageDeliveryStatus.fromTwilio(rawStatus);
        message.updateStatus(status);
        if (message.getChannel() == MessageChannelType.WHATSAPP
                && strategy == NotificationStrategy.WHATSAPP_THEN_SMS
                && (status == MessageDeliveryStatus.FAILED || status == MessageDeliveryStatus.UNDELIVERED)) {
            Journey journey = journeys.findByIdForUpdate(message.getJourney().getId()).orElseThrow();
            if (journey.hasNotificationConsent() && journey.getState() == JourneyState.ACCEPTED) {
                sendSmsFallback(journey, messageBody());
            }
        }
    }

    @Transactional
    public void runMessageFallback(UUID journeyId) {
        Journey journey = journeys.findByIdForUpdate(journeyId).orElseThrow();
        if (!journey.hasNotificationConsent() || journey.getState() != JourneyState.ACCEPTED
                || strategy != NotificationStrategy.WHATSAPP_THEN_SMS) return;
        JourneyMessage whatsappMessage = messages.findByJourneyAndChannelForUpdate(journeyId, MessageChannelType.WHATSAPP).orElse(null);
        if (whatsappMessage == null || whatsappMessage.getStatus() == MessageDeliveryStatus.DELIVERED
                || whatsappMessage.getStatus() == MessageDeliveryStatus.READ) return;
        sendSmsFallback(journey, messageBody());
    }

    private void sendSmsFallback(Journey journey, String body) {
        if (rideLink == null || rideLink.isBlank()) return;
        sendOnce(journey, MessageChannelType.SMS, sms, body);
    }

    private Dispatch sendOnce(Journey journey, MessageChannelType type, MessageChannel channel, String body) {
        return sendOnce(journey, type, channel, body, Map.of());
    }

    private Dispatch sendOnce(Journey journey, MessageChannelType type, MessageChannel channel, String body,
                              Map<String, String> templateVariables) {
        JourneyMessage existing = messages.findByJourneyAndChannelForUpdate(journey.getId(), type).orElse(null);
        if (existing != null) return new Dispatch(existing, false);
        JourneyMessage record = messages.saveAndFlush(new JourneyMessage(journey, type));
        try {
            String sid = channel.send(journey.getTravellerPhone(), body, templateVariables);
            if (sid == null || sid.isBlank()) record.updateStatus(MessageDeliveryStatus.FAILED);
            else record.assignProviderMessageId(sid);
        } catch (RuntimeException exception) {
            record.updateStatus(MessageDeliveryStatus.FAILED);
            log.warn("{} notification submission failed for journey {}", type, journey.getId());
        }
        return new Dispatch(record, true);
    }

    private String messageBody() {
        return "Your ride link: " + rideLink + ". Open it to confirm your ride.";
    }

    private record Dispatch(JourneyMessage message, boolean created) { }
}
