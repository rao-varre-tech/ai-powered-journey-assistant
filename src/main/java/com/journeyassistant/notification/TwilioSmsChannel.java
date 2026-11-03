package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;

public class TwilioSmsChannel implements MessageChannel {
    private static final Logger log = LoggerFactory.getLogger(TwilioSmsChannel.class);

    private final TwilioMessageCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final URI statusCallback;
    private final boolean trialMode;

    public TwilioSmsChannel(TwilioMessageCreatorFactory creatorFactory, TwilioRestClient restClient,
                            String fromNumber, URI statusCallback, boolean trialMode) {
        this.creatorFactory = creatorFactory; this.restClient = restClient;
        this.fromNumber = fromNumber; this.statusCallback = statusCallback;
        this.trialMode = trialMode;
    }

    @Override public String send(String toPhone, String message) {
        log.info("Submitting Twilio SMS with parameters {}", trialMode
                ? "[To, From, Body]" : "[To, From, Body, StatusCallback]");
        URI callback = trialMode ? null : statusCallback;
        return creatorFactory.sms(toPhone, fromNumber, message, callback).create(restClient).getSid();
    }

    @Override public MessageChannelType channelType() { return MessageChannelType.SMS; }
}
