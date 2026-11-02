package com.journeyassistant.notification;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IvrTwimlFactoryTest {
    private final IvrTwimlFactory factory = new IvrTwimlFactory();
    private final UUID id = UUID.fromString("77f98487-5c13-489e-8f18-d282a9e72d5a");

    @Test void voicePromptContainsGatherAndTimeoutFallback() {
        String xml = factory.voicePrompt(id);
        assertTrue(xml.contains("numDigits=\"1\"")); assertTrue(xml.contains("timeout=\"8\""));
        assertTrue(xml.contains("/webhooks/twilio/gather?journeyId=" + id));
        assertTrue(xml.contains(IvrTwimlFactory.PROMPT)); assertTrue(xml.contains("Goodbye."));
    }

    @Test void eachDigitReturnsConfirmationAndHangup() {
        assertTrue(factory.responseForDigit("1", id, 0).contains("ride link will be sent shortly"));
        assertTrue(factory.responseForDigit("2", id, 0).contains("five minutes"));
        assertTrue(factory.responseForDigit("3", id, 0).toLowerCase().contains("any more calls"),
                factory.responseForDigit("3", id, 0));
        for (String digit : new String[]{"1", "2", "3"}) assertTrue(factory.responseForDigit(digit, id, 0).contains("<Hangup"));
    }

    @Test void invalidDigitRepeatsPromptOnceThenHangsUp() {
        assertTrue(factory.responseForDigit("9", id, 1).contains(IvrTwimlFactory.PROMPT));
        String second = factory.responseForDigit("8", id, 2);
        assertTrue(!second.contains("<Gather")); assertTrue(second.contains("<Hangup"));
    }
}
