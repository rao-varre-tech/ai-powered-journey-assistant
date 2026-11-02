package com.journeyassistant.config;

import com.twilio.http.TwilioRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${ivr.provider:fake}' == 'twilio' or '${notification.provider:fake}' == 'twilio'")
public class TwilioClientConfiguration {
    @Bean
    TwilioRestClient twilioRestClient(@Value("${twilio.account-sid:}") String accountSid,
                                      @Value("${twilio.auth-token:}") String authToken) {
        if (accountSid.isBlank() || authToken.isBlank())
            throw new IllegalStateException("TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN are required");
        return new TwilioRestClient.Builder(accountSid, authToken).build();
    }
}
