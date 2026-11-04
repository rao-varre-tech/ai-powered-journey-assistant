package com.journeyassistant.config;

import com.journeyassistant.notification.FakeIvrClient;
import com.journeyassistant.notification.IvrClient;
import com.journeyassistant.notification.TwilioCallCreatorFactory;
import com.journeyassistant.notification.TwilioIvrClient;
import com.journeyassistant.notification.TwilioWebhookSignatureValidator;
import com.journeyassistant.notification.IvrTwimlFactory;
import com.journeyassistant.notification.FakeChannel;
import com.journeyassistant.notification.MessageChannel;
import com.journeyassistant.notification.MessageChannelType;
import com.journeyassistant.notification.TwilioMessageCreatorFactory;
import com.journeyassistant.notification.TwilioSmsChannel;
import com.journeyassistant.notification.TwilioWhatsAppChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
public class IvrConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "ivr", name = "provider", havingValue = "fake")
    IvrClient fakeIvrClient() { return new FakeIvrClient(); }

    @Bean
    @ConditionalOnProperty(prefix = "ivr", name = "provider", havingValue = "twilio")
    IvrClient twilioIvrClient(TwilioCallCreatorFactory creatorFactory,
            com.twilio.http.TwilioRestClient restClient,
            @Value("${twilio.from-number}") String fromNumber,
            @Value("${twilio.public-base-url}") String publicBaseUrl,
            @Value("${twilio.trial-mode:false}") boolean trialMode) {
        if (fromNumber.isBlank() || publicBaseUrl.isBlank())
            throw new IllegalStateException("Twilio configuration is incomplete");
        return new TwilioIvrClient(creatorFactory, restClient, fromNumber, URI.create(publicBaseUrl), trialMode);
    }

    @Bean
    TwilioCallCreatorFactory twilioCallCreatorFactory() { return new TwilioCallCreatorFactory(); }

    @Bean
    IvrTwimlFactory ivrTwimlFactory(@Value("${twilio.public-base-url:}") String publicBaseUrl) {
        return new IvrTwimlFactory(publicBaseUrl);
    }

    @Bean
    TwilioMessageCreatorFactory twilioMessageCreatorFactory(ObjectMapper objectMapper) {
        return new TwilioMessageCreatorFactory(objectMapper);
    }

    @Bean("smsChannel")
    @ConditionalOnProperty(prefix = "notification", name = "provider", havingValue = "fake", matchIfMissing = true)
    MessageChannel fakeSmsChannel() { return new FakeChannel(MessageChannelType.SMS); }

    @Bean("whatsappChannel")
    @ConditionalOnProperty(prefix = "notification", name = "provider", havingValue = "fake", matchIfMissing = true)
    MessageChannel fakeWhatsAppChannel() { return new FakeChannel(MessageChannelType.WHATSAPP); }

    @Bean("smsChannel")
    @ConditionalOnProperty(prefix = "notification", name = "provider", havingValue = "twilio")
    MessageChannel twilioSmsChannel(TwilioMessageCreatorFactory factory, com.twilio.http.TwilioRestClient restClient,
            @Value("${twilio.sms-from:}") String fromNumber, @Value("${twilio.public-base-url:}") String publicBaseUrl,
            @Value("${twilio.trial-mode:false}") boolean trialMode) {
        requireMessageConfig(fromNumber, publicBaseUrl, "TWILIO_SMS_FROM");
        return new TwilioSmsChannel(factory, restClient, fromNumber,
                URI.create(publicBaseUrl.replaceAll("/$", "") + "/webhooks/twilio/message-status"), trialMode);
    }

    @Bean("whatsappChannel")
    @ConditionalOnProperty(prefix = "notification", name = "provider", havingValue = "twilio")
    MessageChannel twilioWhatsAppChannel(TwilioMessageCreatorFactory factory, com.twilio.http.TwilioRestClient restClient,
            @Value("${twilio.whatsapp-from:}") String fromNumber,
            @Value("${twilio.whatsapp-content-sid:}") String contentSid,
            @Value("${twilio.public-base-url:}") String publicBaseUrl,
            @Value("${twilio.trial-mode:false}") boolean trialMode) {
        requireMessageConfig(fromNumber, publicBaseUrl, "TWILIO_WHATSAPP_FROM");
        return new TwilioWhatsAppChannel(factory, restClient, fromNumber, contentSid,
                URI.create(publicBaseUrl.replaceAll("/$", "") + "/webhooks/twilio/message-status"), trialMode);
    }

    @Bean
    TwilioWebhookSignatureValidator signatureValidator(
            @Value("${ivr.provider:fake}") String provider,
            @Value("${notification.provider:fake}") String notificationProvider,
            @Value("${ivr.signature-validation-enabled:false}") boolean enabled,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.public-base-url:}") String publicBaseUrl) {
        // if (!enabled && (!"fake".equalsIgnoreCase(provider) || !"fake".equalsIgnoreCase(notificationProvider)))
        //     throw new IllegalStateException("Twilio signature validation cannot be disabled outside the fake provider");
        if (enabled && (authToken.isBlank() || publicBaseUrl.isBlank()))
            throw new IllegalStateException("Signature validation requires TWILIO_AUTH_TOKEN and PUBLIC_BASE_URL");
        return new TwilioWebhookSignatureValidator(enabled, authToken, publicBaseUrl);
    }

    private void requireMessageConfig(String fromNumber, String publicBaseUrl, String fromVariable) {
        if (fromNumber.isBlank() || publicBaseUrl.isBlank())
            throw new IllegalStateException(fromVariable + " and PUBLIC_BASE_URL are required for Twilio messaging");
    }
}
