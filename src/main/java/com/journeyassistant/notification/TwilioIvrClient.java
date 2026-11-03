package com.journeyassistant.notification;

import com.twilio.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.http.TwilioRestClient;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class TwilioIvrClient implements IvrClient {
    private static final Logger log = LoggerFactory.getLogger(TwilioIvrClient.class);

    private final TwilioCallCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final URI publicBaseUrl;
    private final boolean trialMode;

    public TwilioIvrClient(TwilioCallCreatorFactory creatorFactory, TwilioRestClient restClient,
                           String fromNumber, URI publicBaseUrl, boolean trialMode) {
        this.creatorFactory = creatorFactory;
        this.restClient = restClient;
        this.fromNumber = fromNumber;
        this.publicBaseUrl = publicBaseUrl;
        this.trialMode = trialMode;
    }

    @Override
    public String placeCall(UUID journeyId, String toPhone) {
        URI voiceUrl = publicBaseUrl.resolve("/webhooks/twilio/voice?journeyId=" + journeyId);
        CallCreator creator = creatorFactory.create(toPhone, fromNumber, voiceUrl);
        if (!trialMode) {
            URI statusUrl = publicBaseUrl.resolve("/webhooks/twilio/status");
            creator.setStatusCallback(statusUrl)
                    .setStatusCallbackEvent(List.of("completed", "no-answer", "busy", "failed"))
                    .setStatusCallbackMethod(HttpMethod.POST);
        }
        log.info("Submitting Twilio call for journey {} with parameters {}", journeyId,
                trialMode ? "[To, From, Url]" : "[To, From, Url, StatusCallback, StatusCallbackEvent, StatusCallbackMethod]");
        String callSid = creator.create(restClient).getSid();
        log.info("Twilio call created for journey {} with Call SID {}", journeyId, callSid);
        return callSid;
    }
}
