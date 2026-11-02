package com.journeyassistant.notification;

import com.twilio.http.HttpMethod;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.http.TwilioRestClient;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class TwilioIvrClient implements IvrClient {
    private final TwilioCallCreatorFactory creatorFactory;
    private final TwilioRestClient restClient;
    private final String fromNumber;
    private final URI publicBaseUrl;

    public TwilioIvrClient(TwilioCallCreatorFactory creatorFactory, TwilioRestClient restClient,
                           String fromNumber, URI publicBaseUrl) {
        this.creatorFactory = creatorFactory;
        this.restClient = restClient;
        this.fromNumber = fromNumber;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String placeCall(UUID journeyId, String toPhone) {
        URI voiceUrl = publicBaseUrl.resolve("/webhooks/twilio/voice?journeyId=" + journeyId);
        URI statusUrl = publicBaseUrl.resolve("/webhooks/twilio/status");
        CallCreator creator = creatorFactory.create(toPhone, fromNumber, voiceUrl)
                .setStatusCallback(statusUrl)
                .setStatusCallbackEvent(List.of("completed", "no-answer", "busy", "failed"))
                .setStatusCallbackMethod(HttpMethod.POST);
        return creator.create(restClient).getSid();
    }
}
