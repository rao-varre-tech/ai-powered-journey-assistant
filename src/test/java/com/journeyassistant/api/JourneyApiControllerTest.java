package com.journeyassistant.api;

import com.journeyassistant.journey.JourneyApplicationService;
import com.journeyassistant.journey.JourneyNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JourneyApiController.class)
class JourneyApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JourneyApplicationService journeyService;

    @Test
    void rejectsInvalidJourneyInputWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .contentType("application/json")
                        .content("""
                                {
                                  "userPhone": "not-a-phone",
                                  "destinationLatitude": 91,
                                  "destinationLongitude": 181,
                                  "geofenceRadiusMeters": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(journeyService);
    }

    @Test
    void mapsMissingJourneyToNotFoundResponse() throws Exception {
        UUID journeyId = UUID.randomUUID();
        when(journeyService.getJourney(journeyId)).thenThrow(new JourneyNotFoundException(journeyId));

        mockMvc.perform(get("/api/v1/journeys/{journeyId}", journeyId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/journeys/" + journeyId));
    }
}
