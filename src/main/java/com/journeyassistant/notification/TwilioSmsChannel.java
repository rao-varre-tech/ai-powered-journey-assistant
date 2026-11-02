package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import java.net.URI;

public class TwilioSmsChannel implements MessageChannel {
    private final TwilioMessageCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final URI statusCallback;

    public TwilioSmsChannel(TwilioMessageCreatorFactory creatorFactory, TwilioRestClient restClient,
                            String fromNumber, URI statusCallback) {
        this.creatorFactory = creatorFactory; this.restClient = restClient;
        this.fromNumber = fromNumber; this.statusCallback = statusCallback;
    }

    @Override public String send(String toPhone, String message) {
        return creatorFactory.sms(toPhone, fromNumber, message, statusCallback).create(restClient).getSid();
    }

    @Override public MessageChannelType channelType() { return MessageChannelType.SMS; }
}
