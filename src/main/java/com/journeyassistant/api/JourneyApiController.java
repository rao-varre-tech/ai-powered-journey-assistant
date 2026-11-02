package com.journeyassistant.api;

import com.journeyassistant.journey.Journey;
import com.journeyassistant.journey.JourneyApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journeys")
public class JourneyApiController {

    private final JourneyApplicationService journeyService;

    public JourneyApiController(JourneyApplicationService journeyService) {
        this.journeyService = journeyService;
    }

    @PostMapping
    public ResponseEntity<CreateJourneyResponse> createJourney(@Valid @RequestBody CreateJourneyRequest request) {
        Journey journey = journeyService.createJourney(request.userPhone(), request.destinationLatitude(),
                request.destinationLongitude(), request.geofenceRadiusMeters());
        return ResponseEntity.created(URI.create("/api/v1/journeys/" + journey.getId()))
                .body(CreateJourneyResponse.from(journey));
    }

    @PostMapping("/{journeyId}/locations")
    public LocationUpdateResponse updateLocation(@PathVariable UUID journeyId,
                                                @Valid @RequestBody LocationUpdateRequest request) {
        return LocationUpdateResponse.from(journeyService.updateLocation(
                journeyId, request.latitude(), request.longitude(), request.timestamp()));
    }

    @GetMapping("/{journeyId}")
    public JourneyStatusResponse getJourneyStatus(@PathVariable UUID journeyId) {
        return JourneyStatusResponse.from(journeyService.getJourney(journeyId));
    }
}
