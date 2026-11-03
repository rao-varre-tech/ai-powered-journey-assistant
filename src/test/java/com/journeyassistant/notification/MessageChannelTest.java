package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageChannelTest {
    @Test void twilioSmsChannelReturnsTwilioMessageSid() {
        TwilioMessageCreatorFactory factory = mock(TwilioMessageCreatorFactory.class);
        TwilioRestClient rest = mock(TwilioRestClient.class);
        MessageCreator creator = mock(MessageCreator.class);
        Message message = mock(Message.class);
        URI callback = URI.create("https://journeys.example/webhooks/twilio/message-status");
        when(factory.sms("+15550001111", "+15550002222", "hello", callback)).thenReturn(creator);
        when(creator.create(rest)).thenReturn(message);
        when(message.getSid()).thenReturn("SM-SMS");

        TwilioSmsChannel channel = new TwilioSmsChannel(factory, rest, "+15550002222", callback, false);
        assertEquals("SM-SMS", channel.send("+15550001111", "hello"));
        verify(factory).sms("+15550001111", "+15550002222", "hello", callback);
    }

    @Test void trialModeTwilioSmsOmitsStatusCallback() {
        TwilioMessageCreatorFactory factory = mock(TwilioMessageCreatorFactory.class);
        TwilioRestClient rest = mock(TwilioRestClient.class);
        MessageCreator creator = mock(MessageCreator.class);
        Message message = mock(Message.class);
        URI callback = URI.create("https://journeys.example/webhooks/twilio/message-status");
        when(factory.sms("+15550001111", "+15550002222", "hello", null)).thenReturn(creator);
        when(creator.create(rest)).thenReturn(message);
        when(message.getSid()).thenReturn("SM-TRIAL");

        TwilioSmsChannel channel = new TwilioSmsChannel(factory, rest, "+15550002222", callback, true);

        assertEquals("SM-TRIAL", channel.send("+15550001111", "hello"));
        verify(factory).sms("+15550001111", "+15550002222", "hello", null);
    }

    @Test void twilioWhatsAppChannelReturnsSidAndPassesConfiguredTemplate() {
        TwilioMessageCreatorFactory factory = mock(TwilioMessageCreatorFactory.class);
        TwilioRestClient rest = mock(TwilioRestClient.class);
        MessageCreator creator = mock(MessageCreator.class);
        Message message = mock(Message.class);
        URI callback = URI.create("https://journeys.example/webhooks/twilio/message-status");
        when(factory.whatsapp("+15550001111", "+15550002222", "hello", "HX-template", null, callback)).thenReturn(creator);
        when(creator.create(rest)).thenReturn(message);
        when(message.getSid()).thenReturn("SM-WA");

        TwilioWhatsAppChannel channel = new TwilioWhatsAppChannel(factory, rest, "+15550002222", "HX-template", callback, false);
        assertEquals("SM-WA", channel.send("+15550001111", "hello"));
        verify(factory).whatsapp("+15550001111", "+15550002222", "hello", "HX-template", null, callback);
        assertEquals("whatsapp:+15550001111", TwilioMessageCreatorFactory.whatsappAddress("+15550001111"));
        assertEquals("whatsapp:+15550002222", TwilioMessageCreatorFactory.whatsappAddress("whatsapp:+15550002222"));
    }

    @Test void trialModeTwilioWhatsAppUsesBodyWithoutTemplateOrStatusCallback() {
        TwilioMessageCreatorFactory factory = mock(TwilioMessageCreatorFactory.class);
        TwilioRestClient rest = mock(TwilioRestClient.class);
        MessageCreator creator = mock(MessageCreator.class);
        Message message = mock(Message.class);
        URI callback = URI.create("https://journeys.example/webhooks/twilio/message-status");
        when(factory.whatsapp("+15550001111", "+15550002222", "hello", null, null, null)).thenReturn(creator);
        when(creator.create(rest)).thenReturn(message);
        when(message.getSid()).thenReturn("SM-WA-TRIAL");

        TwilioWhatsAppChannel channel = new TwilioWhatsAppChannel(factory, rest, "+15550002222",
                "HX-template", callback, true);

        assertEquals("SM-WA-TRIAL", channel.send("+15550001111", "hello"));
        verify(factory).whatsapp("+15550001111", "+15550002222", "hello", null, null, null);
    }

    @Test void templateVariablesAreJsonEncoded() {
        TwilioMessageCreatorFactory factory = new TwilioMessageCreatorFactory(new com.fasterxml.jackson.databind.ObjectMapper());
        assertEquals("{\"1\":\"https://ride.example/code\"}",
                factory.serializeContentVariables(java.util.Map.of("1", "https://ride.example/code")));
    }
}
