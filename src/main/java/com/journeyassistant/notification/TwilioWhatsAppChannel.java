package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.Map;

public class TwilioWhatsAppChannel implements MessageChannel {
    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppChannel.class);

    private final TwilioMessageCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final String contentSid;
    private final URI statusCallback;
    private final boolean trialMode;

    public TwilioWhatsAppChannel(TwilioMessageCreatorFactory creatorFactory, TwilioRestClient restClient,
                                 String fromNumber, String contentSid, URI statusCallback, boolean trialMode) {
        this.creatorFactory = creatorFactory; this.restClient = restClient;
        this.fromNumber = fromNumber; this.contentSid = contentSid; this.statusCallback = statusCallback;
        this.trialMode = trialMode;
    }

    @Override public String send(String toPhone, String message) {
        return send(toPhone, message, Map.of("1", message));
    }

    @Override public String send(String toPhone, String message, Map<String, String> templateVariables) {
        String effectiveContentSid = trialMode ? null : contentSid;
        String encodedVariables = effectiveContentSid == null || effectiveContentSid.isBlank() ? null
                : creatorFactory.serializeContentVariables(templateVariables);
        log.info("Submitting Twilio WhatsApp message with parameters {}", trialMode
                ? "[To, From, Body]"
                : (effectiveContentSid == null || effectiveContentSid.isBlank()
                    ? "[To, From, Body, StatusCallback]"
                    : "[To, From, ContentSid, ContentVariables, StatusCallback]"));
        URI callback = trialMode ? null : statusCallback;
        return creatorFactory.whatsapp(toPhone, fromNumber, message, effectiveContentSid, encodedVariables, callback)
                .create(restClient).getSid();
    }

    @Override public MessageChannelType channelType() { return MessageChannelType.WHATSAPP; }
}
