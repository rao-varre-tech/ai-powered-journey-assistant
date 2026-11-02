package com.journeyassistant.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class FakeIvrClient implements IvrClient {
    private static final Logger log = LoggerFactory.getLogger(FakeIvrClient.class);

    @Override
    public String placeCall(UUID journeyId, String toPhone) {
        String callSid = "FAKE-" + UUID.randomUUID();
        log.info("Fake IVR call queued for journey {} to phone {}", journeyId, maskPhone(toPhone));
        return callSid;
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "****";
        String digits = phone.replaceAll("\\D", "");
        return "****" + digits.substring(Math.max(0, digits.length() - 4));
    }
}
