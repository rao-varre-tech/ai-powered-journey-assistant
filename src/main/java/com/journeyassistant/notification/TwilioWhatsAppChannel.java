package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import java.net.URI;
import java.util.Map;

public class TwilioWhatsAppChannel implements MessageChannel {
    private final TwilioMessageCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final String contentSid;
    private final URI statusCallback;

    public TwilioWhatsAppChannel(TwilioMessageCreatorFactory creatorFactory, TwilioRestClient restClient,
                                 String fromNumber, String contentSid, URI statusCallback) {
        this.creatorFactory = creatorFactory; this.restClient = restClient;
        this.fromNumber = fromNumber; this.contentSid = contentSid; this.statusCallback = statusCallback;
    }

    @Override public String send(String toPhone, String message) {
        return send(toPhone, message, Map.of("1", message));
    }

    @Override public String send(String toPhone, String message, Map<String, String> templateVariables) {
        String encodedVariables = contentSid == null || contentSid.isBlank() ? null
                : creatorFactory.serializeContentVariables(templateVariables);
        return creatorFactory.whatsapp(toPhone, fromNumber, message, contentSid, encodedVariables, statusCallback)
                .create(restClient).getSid();
    }

    @Override public MessageChannelType channelType() { return MessageChannelType.WHATSAPP; }
}
