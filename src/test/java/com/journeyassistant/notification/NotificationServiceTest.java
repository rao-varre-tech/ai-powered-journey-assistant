package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.journey.JourneyState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private final UUID journeyId = UUID.randomUUID();

    @Test void smsStrategySendsOnlySms() {
        Fixture f = new Fixture("sms");
        f.service.sendRideLink(journeyId);
        verify(f.sms).send(eq("+15550001111"), eq(body()), eq(Map.of()));
        verify(f.whatsapp, never()).send(any(), any(), anyMap());
    }

    @Test void whatsappStrategySendsOnlyWhatsApp() {
        Fixture f = new Fixture("whatsapp");
        f.service.sendRideLink(journeyId);
        verify(f.whatsapp).send(eq("+15550001111"), eq(body()), eq(templateVariables()));
        verify(f.sms, never()).send(any(), any(), anyMap());
    }

    @Test void bothStrategySendsAndRecordsWhatsAppThenSms() {
        Fixture f = new Fixture("both");
        f.service.sendRideLink(journeyId);
        var order = inOrder(f.whatsapp, f.sms);
        order.verify(f.whatsapp).send(eq("+15550001111"), eq(body()), eq(templateVariables()));
        order.verify(f.sms).send(eq("+15550001111"), eq(body()), eq(Map.of()));
        assertEquals(2, f.records.size());
    }

    @Test void whatsappThenSmsSchedulesFallbackButInitiallySendsOnlyWhatsApp() {
        Fixture f = new Fixture("whatsapp_then_sms");
        f.service.sendRideLink(journeyId);
        verify(f.whatsapp).send(eq("+15550001111"), eq(body()), eq(templateVariables()));
        verify(f.sms, never()).send(any(), any(), anyMap());
        verify(f.scheduler).scheduleMessageFallback(journeyId, Duration.ofSeconds(60));
    }

    @Test void trialModeWhatsappThenSmsUsesTimerBasedFallback() {
        Fixture f = new Fixture("whatsapp_then_sms", true);
        f.service.sendRideLink(journeyId);
        verify(f.whatsapp).send(eq("+15550001111"), eq(body()), eq(templateVariables()));
        verify(f.sms, never()).send(any(), any(), anyMap());
        verify(f.scheduler).scheduleMessageFallback(journeyId, Duration.ofSeconds(60));
    }

    @ParameterizedTest
    @CsvSource({"failed, FAILED", "undelivered, UNDELIVERED"})
    void failedOrUndeliveredStatusTriggersSmsOnce(String raw, String enumName) {
        Fixture f = new Fixture("whatsapp_then_sms");
        JourneyMessage wa = new JourneyMessage(f.journey, MessageChannelType.WHATSAPP);
        wa.assignProviderMessageId("SM-WA");
        f.records.put(MessageChannelType.WHATSAPP, wa);

        f.service.processStatus("SM-WA", raw);
        f.service.processStatus("SM-WA", raw);

        assertEquals(MessageDeliveryStatus.valueOf(enumName), wa.getStatus());
        verify(f.sms).send(eq("+15550001111"), eq(body()), eq(Map.of()));
    }

    @Test void timeoutTriggersFallbackUnlessWhatsAppWasDelivered() {
        Fixture f = new Fixture("whatsapp_then_sms");
        JourneyMessage wa = new JourneyMessage(f.journey, MessageChannelType.WHATSAPP);
        wa.assignProviderMessageId("SM-WA");
        wa.updateStatus(MessageDeliveryStatus.SENT);
        f.records.put(MessageChannelType.WHATSAPP, wa);
        f.service.runMessageFallback(journeyId);
        f.service.runMessageFallback(journeyId);
        verify(f.sms).send(eq("+15550001111"), eq(body()), eq(Map.of()));

        Fixture delivered = new Fixture("whatsapp_then_sms");
        JourneyMessage deliveredWa = new JourneyMessage(delivered.journey, MessageChannelType.WHATSAPP);
        deliveredWa.assignProviderMessageId("SM-DELIVERED");
        deliveredWa.updateStatus(MessageDeliveryStatus.DELIVERED);
        delivered.records.put(MessageChannelType.WHATSAPP, deliveredWa);
        delivered.service.runMessageFallback(journeyId);
        verify(delivered.sms, never()).send(any(), any(), anyMap());
    }

    @Test void consentFalsePreventsMessagesAndTimeoutFallback() {
        Fixture f = new Fixture("both");
        when(f.journey.hasNotificationConsent()).thenReturn(false);
        f.service.sendRideLink(journeyId);
        f.service.runMessageFallback(journeyId);
        verify(f.sms, never()).send(any(), any(), anyMap());
        verify(f.whatsapp, never()).send(any(), any(), anyMap());
    }

    @Test void statusNeverDowngradesDeliveredToSent() {
        Journey journey = new Journey("+15550001111", 0, 0, 50, true);
        JourneyMessage message = new JourneyMessage(journey, MessageChannelType.SMS);
        assertEquals(true, message.updateStatus(MessageDeliveryStatus.DELIVERED));
        assertEquals(false, message.updateStatus(MessageDeliveryStatus.SENT));
        assertEquals(MessageDeliveryStatus.DELIVERED, message.getStatus());
    }

    private String body() { return "Your ride link: https://ride.example/abc. Open it to confirm your ride."; }
    private Map<String, String> templateVariables() { return Map.of("1", "https://ride.example/abc"); }

    private final class Fixture {
        final Journey journey = mock(Journey.class);
        final JourneyRepository journeys = mock(JourneyRepository.class);
        final JourneyMessageRepository repository = mock(JourneyMessageRepository.class);
        final MessageChannel sms = mock(MessageChannel.class);
        final MessageChannel whatsapp = mock(MessageChannel.class);
        final RetryScheduler scheduler = mock(RetryScheduler.class);
        final Map<MessageChannelType, JourneyMessage> records = new HashMap<>();
        final NotificationService service;

        Fixture(String strategy) {
            this(strategy, false);
        }

        Fixture(String strategy, boolean trialMode) {
            when(journeys.findByIdForUpdate(journeyId)).thenReturn(Optional.of(journey));
            when(repository.findByJourneyAndChannelForUpdate(any(), any())).thenAnswer(invocation ->
                    Optional.ofNullable(records.get(invocation.getArgument(1))));
            when(repository.findByProviderMessageIdForUpdate(any())).thenAnswer(invocation -> records.values().stream()
                    .filter(message -> invocation.getArgument(0).equals(message.getProviderMessageId())).findFirst());
            when(repository.findByProviderMessageId(any())).thenAnswer(invocation -> records.values().stream()
                    .filter(message -> invocation.getArgument(0).equals(message.getProviderMessageId())).findFirst());
            when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
                JourneyMessage record = invocation.getArgument(0);
                records.put(record.getChannel(), record);
                return record;
            });
            when(journey.getId()).thenReturn(journeyId);
            when(journey.getState()).thenReturn(JourneyState.ACCEPTED);
            when(journey.hasNotificationConsent()).thenReturn(true);
            when(journey.getTravellerPhone()).thenReturn("+15550001111");
            when(sms.send(any(), any(), anyMap())).thenAnswer(invocation -> "SMS-" + UUID.randomUUID());
            when(whatsapp.send(any(), any(), anyMap())).thenAnswer(invocation -> "WA-" + UUID.randomUUID());
            service = new NotificationService(journeys, repository, sms, whatsapp, scheduler, strategy,
                    Duration.ofSeconds(60), "https://ride.example/abc", trialMode);
        }
    }

}
