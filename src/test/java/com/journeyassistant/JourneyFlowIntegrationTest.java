package com.journeyassistant;

import com.journeyassistant.journey.JourneyRepository;
import com.journeyassistant.notification.IvrClient;
import com.journeyassistant.notification.JourneyMessageRepository;
import com.journeyassistant.notification.MessageChannelType;
import com.journeyassistant.notification.NotificationAttemptRepository;
import com.journeyassistant.notification.RetryScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "notification.provider=fake",
        "notification.strategy=sms",
        "notification.ride-link=https://ride.example/abc",
        "ivr.provider=fake",
        "ivr.signature-validation-enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class JourneyFlowIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JourneyRepository journeys;
    @Autowired NotificationAttemptRepository attempts;
    @Autowired JourneyMessageRepository messages;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean IvrClient ivrClient;
    @MockitoBean RetryScheduler scheduler;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM journey_messages");
        jdbc.update("DELETE FROM notification_attempts");
        jdbc.update("DELETE FROM location_updates");
        jdbc.update("DELETE FROM journeys");
        clearInvocations(ivrClient, scheduler);
        when(ivrClient.placeCall(any(UUID.class), any(String.class))).thenReturn("CALL-1", "CALL-2");
    }

    @Test
    void createArrivalRetryAcceptSendsSmsAndPersistsEachStep() throws Exception {
        String createJson = """
                {"userPhone":"+15550001111","destinationLatitude":12.0,"destinationLongitude":77.0,
                 "geofenceRadiusMeters":200,"notificationConsent":true}
                """;
        String created = mvc.perform(post("/api/v1/journeys").contentType(MediaType.APPLICATION_JSON).content(createJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID journeyId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(created).get("journeyId").asText());

        mvc.perform(post("/api/v1/journeys/{id}/locations", journeyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitude":12.0,"longitude":77.0,"timestamp":"%s"}
                                """.formatted(Instant.now().toString())))
                .andExpect(status().isOk());

        assertThat(journeys.findById(journeyId).orElseThrow().getState().name()).isEqualTo("CALLING");
        assertThat(attempts.findAllByJourney_IdOrderByAttemptNumberAsc(journeyId)).hasSize(1);
        verify(ivrClient).placeCall(journeyId, "+15550001111");

        mvc.perform(post("/webhooks/twilio/gather").queryParam("journeyId", journeyId.toString())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("CallSid", "CALL-1").param("Digits", "2"))
                .andExpect(status().isOk());
        assertThat(journeys.findById(journeyId).orElseThrow().getState().name()).isEqualTo("RETRY_SCHEDULED");
        verify(scheduler).scheduleRetry(journeyId, 1, Duration.ofMinutes(5));

        mvc.perform(post("/internal/tasks/journeys/{id}/retry", journeyId))
                .andExpect(status().isNoContent());
        assertThat(journeys.findById(journeyId).orElseThrow().getState().name()).isEqualTo("CALLING");
        assertThat(attempts.findAllByJourney_IdOrderByAttemptNumberAsc(journeyId)).hasSize(2);
        verify(ivrClient, times(2)).placeCall(eq(journeyId), eq("+15550001111"));

        mvc.perform(post("/webhooks/twilio/gather").queryParam("journeyId", journeyId.toString())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("CallSid", "CALL-2").param("Digits", "1"))
                .andExpect(status().isOk());

        assertThat(journeys.findById(journeyId).orElseThrow().getState().name()).isEqualTo("ACCEPTED");
        var persistedMessages = messages.findAll();
        assertThat(persistedMessages).hasSize(1);
        assertThat(persistedMessages.getFirst().getChannel()).isEqualTo(MessageChannelType.SMS);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM journey_messages WHERE journey_id = ? AND channel = 'SMS'",
                Integer.class, journeyId)).isEqualTo(1);
    }
}
