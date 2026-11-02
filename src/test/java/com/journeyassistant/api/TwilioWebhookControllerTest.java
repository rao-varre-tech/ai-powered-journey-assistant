package com.journeyassistant.api;

import com.journeyassistant.notification.IvrTwimlFactory;
import com.journeyassistant.notification.IvrWebhookService;
import com.journeyassistant.notification.TwilioWebhookSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class TwilioWebhookControllerTest {
    private static final String TOKEN = "test-auth-token";
    private final UUID id = UUID.fromString("77f98487-5c13-489e-8f18-d282a9e72d5a");
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TwilioWebhookController(new IvrTwimlFactory(),
                mock(IvrWebhookService.class), new TwilioWebhookSignatureValidator(true, TOKEN, "https://journey.example"))).build();
    }

    @Test void acceptsValidTwilioSignature() throws Exception {
        String url = "https://journey.example/webhooks/twilio/voice?journeyId=" + id;
        mvc.perform(post("/webhooks/twilio/voice").queryParam("journeyId", id.toString())
                        .header("X-Twilio-Signature", signature(url))
                        .contentType("application/x-www-form-urlencoded"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<Gather")));
    }

    @Test void rejectsInvalidTwilioSignature() throws Exception {
        mvc.perform(post("/webhooks/twilio/voice").queryParam("journeyId", id.toString())
                        .header("X-Twilio-Signature", "invalid")
                        .contentType("application/x-www-form-urlencoded"))
                .andExpect(status().isForbidden());
    }

    private String signature(String url) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(url.getBytes(StandardCharsets.UTF_8)));
    }
}
