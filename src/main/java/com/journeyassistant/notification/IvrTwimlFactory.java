package com.journeyassistant.notification;

import com.twilio.http.HttpMethod;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Gather;
import com.twilio.twiml.voice.Hangup;
import com.twilio.twiml.voice.Say;
import java.util.UUID;

public class IvrTwimlFactory {
    public static final String PROMPT = "You have reached your destination. Press 1 to get a ride link, press 2 to be called again in 5 minutes, press 3 to stop calls.";

    public String voicePrompt(UUID journeyId) {
        return new VoiceResponse.Builder()
                .gather(gather(journeyId, new Say.Builder(PROMPT).build()))
                .say(new Say.Builder("We didn't receive a response. Goodbye.").build())
                .hangup(new Hangup.Builder().build())
                .build().toXml();
    }

    public String responseForDigit(String digit, UUID journeyId, int invalidCount) {
        if (!"1".equals(digit) && !"2".equals(digit) && !"3".equals(digit)) {
            if (invalidCount < 2) return new VoiceResponse.Builder()
                    .gather(gather(journeyId, new Say.Builder(PROMPT).build()))
                    .say(new Say.Builder("Sorry, that response was not valid. Goodbye.").build())
                    .hangup(new Hangup.Builder().build()).build().toXml();
            return new VoiceResponse.Builder().say(new Say.Builder("Sorry, that response was not valid. Goodbye.").build())
                    .hangup(new Hangup.Builder().build()).build().toXml();
        }
        String confirmation = switch (digit) {
            case "1" -> "Thank you. You will receive your ride link by SMS. Goodbye.";
            case "2" -> "We will call you again in five minutes. Goodbye.";
            default -> "You will not receive any more calls for this journey. Goodbye.";
        };
        return new VoiceResponse.Builder().say(new Say.Builder(confirmation).build())
                .hangup(new Hangup.Builder().build()).build().toXml();
    }

    private Gather gather(UUID id, Say say) {
        return new Gather.Builder().numDigits(1).timeout(8).action("/webhooks/twilio/gather?journeyId=" + id)
                .method(HttpMethod.POST).say(say).build();
    }
}
