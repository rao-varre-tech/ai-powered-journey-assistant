package com.journeyassistant.notification;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.CallCreator;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwilioIvrClientTest {
    @Test void createsCallUsingInjectedMockCreator() {
        TwilioCallCreatorFactory factory = mock(TwilioCallCreatorFactory.class);
        TwilioRestClient rest = mock(TwilioRestClient.class);
        CallCreator creator = mock(CallCreator.class);
        Call call = mock(Call.class);
        UUID journeyId = UUID.fromString("77f98487-5c13-489e-8f18-d282a9e72d5a");
        when(factory.create("+15550001111", "+15550002222", URI.create("https://public.test/webhooks/twilio/voice?journeyId=" + journeyId))).thenReturn(creator);
        when(creator.setStatusCallback(any(URI.class))).thenReturn(creator);
        when(creator.setStatusCallbackEvent(anyList())).thenReturn(creator);
        when(creator.setStatusCallbackMethod(any())).thenReturn(creator);
        when(creator.create(rest)).thenReturn(call);
        when(call.getSid()).thenReturn("CA123");

        TwilioIvrClient client = new TwilioIvrClient(factory, rest, "+15550002222", URI.create("https://public.test"));
        assertEquals("CA123", client.placeCall(journeyId, "+15550001111"));
        verify(creator).setStatusCallback(URI.create("https://public.test/webhooks/twilio/status"));
        verify(creator).create(rest);
    }
}
