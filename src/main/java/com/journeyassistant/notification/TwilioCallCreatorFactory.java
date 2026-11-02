package com.journeyassistant.notification;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.type.PhoneNumber;
import java.net.URI;

public class TwilioCallCreatorFactory {
    public CallCreator create(String to, String from, URI voiceUrl) {
        return Call.creator(new PhoneNumber(to), new PhoneNumber(from), voiceUrl);
    }
}
