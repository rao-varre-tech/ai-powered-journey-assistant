package com.journeyassistant.api;

import com.journeyassistant.notification.IvrTwimlFactory;
import com.journeyassistant.notification.IvrWebhookService;
import com.journeyassistant.notification.TwilioWebhookSignatureValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/twilio")
public class TwilioWebhookController {
    private final IvrTwimlFactory twiml;
    private final IvrWebhookService webhooks;
    private final TwilioWebhookSignatureValidator signatures;

    public TwilioWebhookController(IvrTwimlFactory twiml, IvrWebhookService webhooks,
                                   TwilioWebhookSignatureValidator signatures) {
        this.twiml = twiml; this.webhooks = webhooks; this.signatures = signatures;
    }

    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> voice(@RequestParam UUID journeyId,
            @RequestParam MultiValueMap<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            HttpServletRequest request) {
        if (!valid(request, params, signature)) return forbidden();
        return xml(twiml.voicePrompt(journeyId));
    }

    @PostMapping(value = "/gather", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> gather(@RequestParam UUID journeyId,
            @RequestParam(name = "Digits", required = false) String digits,
            @RequestParam(name = "CallSid") String callSid,
            @RequestParam MultiValueMap<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            HttpServletRequest request) {
        if (!valid(request, params, signature)) return forbidden();
        return xml(webhooks.processDigit(journeyId, callSid, digits));
    }

    @PostMapping(value = "/status", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> status(@RequestParam(name = "CallSid") String callSid,
            @RequestParam(name = "CallStatus") String status,
            @RequestParam MultiValueMap<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            HttpServletRequest request) {
        if (!valid(request, params, signature)) return forbidden();
        webhooks.processStatus(callSid, status);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body("<Response/>");
    }

    private boolean valid(HttpServletRequest request, MultiValueMap<String, String> params, String signature) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(params);
        // journeyId is part of the URL query string and is already included in the signed URL.
        form.remove("journeyId");
        return signatures.isValid(request.getRequestURI(), request.getQueryString(), form, signature);
    }

    private ResponseEntity<String> xml(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
    }

    private ResponseEntity<String> forbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
}
