package com.journeyassistant.notification;

import java.util.UUID;

public record JourneyProviderReferences(UUID journeyId, String latestCallSid,
                                        String smsMessageSid, String whatsappMessageSid) {
}
