package com.journeyassistant.api;

import com.journeyassistant.notification.JourneyProviderReferences;
import com.journeyassistant.notification.JourneyProviderReferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
@Tag(name = "Journeys")
public class JourneyProviderReferencesController {
    private final JourneyProviderReferencesService references;

    public JourneyProviderReferencesController(JourneyProviderReferencesService references) {
        this.references = references;
    }

    @GetMapping("/{journeyId}/provider-references")
    @Operation(summary = "Get provider IDs for a journey",
            description = "Returns the latest CallSid and available SMS/WhatsApp MessageSids. "
                    + "Run this after triggering a call or message, then copy the IDs into webhook requests. "
                    + "Swagger UI displays these values but does not automatically chain them into another request.")
    public JourneyProviderReferences getProviderReferences(@PathVariable UUID journeyId) {
        return references.getReferences(journeyId);
    }
}
