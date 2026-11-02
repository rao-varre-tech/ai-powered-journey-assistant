package com.journeyassistant.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.net.URI;

public class TwilioMessageCreatorFactory {
    private final ObjectMapper objectMapper;

    public TwilioMessageCreatorFactory(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public MessageCreator sms(String to, String from, String body, URI statusCallback) {
        return Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).setStatusCallback(statusCallback);
    }

    public MessageCreator whatsapp(String to, String from, String body, String contentSid,
                                   String contentVariables, URI statusCallback) {
        MessageCreator creator = Message.creator(new PhoneNumber(whatsappAddress(to)),
                new PhoneNumber(whatsappAddress(from)), contentSid == null || contentSid.isBlank() ? body : null)
                .setStatusCallback(statusCallback);
        if (contentSid != null && !contentSid.isBlank()) {
            creator.setContentSid(contentSid);
            creator.setContentVariables(contentVariables);
        }
        return creator;
    }

    static String whatsappAddress(String phone) {
        return phone.startsWith("whatsapp:") ? phone : "whatsapp:" + phone;
    }

    public String serializeContentVariables(java.util.Map<String, String> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode WhatsApp template content", exception);
        }
    }
}
