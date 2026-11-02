package com.journeyassistant.notification;

import java.util.Map;

public interface MessageChannel {
    String send(String toPhone, String message);

    default String send(String toPhone, String message, Map<String, String> templateVariables) {
        return send(toPhone, message);
    }

    MessageChannelType channelType();
}
