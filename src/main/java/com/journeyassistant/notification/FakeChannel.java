package com.journeyassistant.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class FakeChannel implements MessageChannel {
    private static final Logger log = LoggerFactory.getLogger(FakeChannel.class);
    private final MessageChannelType channel;

    public FakeChannel(MessageChannelType channel) { this.channel = channel; }

    @Override public String send(String toPhone, String message) {
        String messageId = "FAKE-MSG-" + UUID.randomUUID();
        log.info("Fake {} message created for phone {}", channel, FakeIvrClient.maskPhone(toPhone));
        return messageId;
    }

    @Override public MessageChannelType channelType() { return channel; }
}
