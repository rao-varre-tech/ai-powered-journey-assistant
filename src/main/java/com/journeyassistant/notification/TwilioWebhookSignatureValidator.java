package com.journeyassistant.notification;

import com.twilio.security.RequestValidator;
import org.springframework.util.MultiValueMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TwilioWebhookSignatureValidator {
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
        if (!enabled) return true;
        if (signature == null || signature.isBlank()) return false;
        String url = publicBaseUrl + requestUri + (queryString == null || queryString.isBlank() ? "" : "?" + queryString);
        Map<String, String> form = new LinkedHashMap<>();
        parameters.forEach((key, values) -> { if (!values.isEmpty()) form.put(key, values.getFirst()); });
        return validator.validate(url, form, signature);
    }
}
