package com.journeyassistant.config;

import com.journeyassistant.notification.FakeIvrClient;
import com.journeyassistant.notification.IvrClient;
import com.journeyassistant.notification.TwilioCallCreatorFactory;
import com.journeyassistant.notification.TwilioIvrClient;
import com.journeyassistant.notification.TwilioWebhookSignatureValidator;
import com.journeyassistant.notification.IvrTwimlFactory;
import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
public class IvrConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "ivr", name = "provider", havingValue = "fake", matchIfMissing = true)
    IvrClient fakeIvrClient() { return new FakeIvrClient(); }

    @Bean
    @ConditionalOnProperty(prefix = "ivr", name = "provider", havingValue = "twilio")
    IvrClient twilioIvrClient(TwilioCallCreatorFactory creatorFactory,
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber,
            @Value("${twilio.public-base-url}") String publicBaseUrl) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank() || publicBaseUrl.isBlank())
            throw new IllegalStateException("Twilio configuration is incomplete");
        TwilioRestClient client = new TwilioRestClient.Builder(accountSid, authToken).build();
        return new TwilioIvrClient(creatorFactory, client, fromNumber, URI.create(publicBaseUrl));
    }

    @Bean
    TwilioCallCreatorFactory twilioCallCreatorFactory() { return new TwilioCallCreatorFactory(); }

    @Bean
    IvrTwimlFactory ivrTwimlFactory() { return new IvrTwimlFactory(); }

    @Bean
    TwilioWebhookSignatureValidator signatureValidator(
            @Value("${ivr.provider:fake}") String provider,
            @Value("${ivr.signature-validation-enabled:false}") boolean enabled,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.public-base-url:}") String publicBaseUrl) {
        if (!enabled && !"fake".equalsIgnoreCase(provider))
            throw new IllegalStateException("Twilio signature validation cannot be disabled outside the fake provider");
        if (enabled && (authToken.isBlank() || publicBaseUrl.isBlank()))
            throw new IllegalStateException("Signature validation requires TWILIO_AUTH_TOKEN and PUBLIC_BASE_URL");
        return new TwilioWebhookSignatureValidator(enabled, authToken, publicBaseUrl);
    }
}
