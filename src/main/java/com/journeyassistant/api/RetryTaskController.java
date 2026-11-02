package com.journeyassistant.api;

import com.journeyassistant.notification.IvrCallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/internal/tasks/journeys")
public class RetryTaskController {
    private final IvrCallService calls;

    public RetryTaskController(IvrCallService calls) { this.calls = calls; }

    @PostMapping("/{journeyId}/retry")
    public ResponseEntity<Void> runRetry(@PathVariable UUID journeyId) {
        calls.runScheduledRetry(journeyId);
        return ResponseEntity.noContent().build();
    }
}
