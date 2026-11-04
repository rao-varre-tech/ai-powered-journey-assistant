package com.journeyassistant.notification;

import com.twilio.security.RequestValidator;
import org.springframework.util.MultiValueMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwilioWebhookSignatureValidator {
    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookSignatureValidator.class);

    private final boolean enabled;
    private final String publicBaseUrl;
    private final RequestValidator validator;

    public TwilioWebhookSignatureValidator(boolean enabled, String authToken, String publicBaseUrl) {
        this.enabled = enabled;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/$", "");
        this.validator = enabled ? new RequestValidator(authToken) : null;
    }

    public boolean isValid(String requestUri, String queryString, MultiValueMap<String, String> parameters,
                           String signature) {
 log.info("publicBaseUrl: {}", publicBaseUrl);                          
log.info("Request URI: {}", requestUri);
log.info("Query string: {}", queryString);
log.info("Received signature present: {}", signature);
log.info("Form parameters: {}", parameters);
log.info("Signature validation enabled: {}", enabled);
        if (!enabled) return true;
        if (signature == null || signature.isBlank()) return false;
        String url = publicBaseUrl + requestUri + (queryString == null || queryString.isBlank() ? "" : "?" + queryString);
        log.info("Validating Twilio webhook signature for URL {} with parameters {}", url, parameters);
        Map<String, String> form = new LinkedHashMap<>();
        parameters.forEach((key, values) -> { if (!values.isEmpty()) form.put(key, values.getFirst()); });
        return validator.validate(url, form, signature);
    }
}
