package com.portal.event_participation_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.event_participation_service.dto.RegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.portal.event_participation_service.dto.RegistrationStatusUpdateRequest;
import com.portal.event_participation_service.entity.Registration;
import com.portal.event_participation_service.repository.QrSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class EventParticipationServiceApplicationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QrSessionRepository qrSessionRepository;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createRegistration_returnsCreatedWithPendingStatus() throws Exception {
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.registrationId").exists());
    }

    @Test
    void getAllRegistrations_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isOk());
    }

    @Test
    void getRegistrationById_notFound_returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/registrations/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void generateQrSession_returnsCreatedWithActiveLink() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/events/" + eventId + "/qr-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validForMinutes\": 30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.eventId").value(eventId.toString()));
    }

    @Test
    void updateRegistrationStatus_changesStatusToApproved() throws Exception {
    RegistrationRequest createRequest = new RegistrationRequest(UUID.randomUUID(), UUID.randomUUID());

    String createResponse = mockMvc.perform(post("/api/v1/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    String registrationId = created.get("registrationId").asText();

    RegistrationStatusUpdateRequest statusUpdate =
            new RegistrationStatusUpdateRequest(Registration.RegistrationStatus.APPROVED);

    mockMvc.perform(patch("/api/v1/registrations/" + registrationId + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
}

@Test
void deleteRegistration_removesItSuccessfully() throws Exception {
    RegistrationRequest createRequest = new RegistrationRequest(UUID.randomUUID(), UUID.randomUUID());

    String createResponse = mockMvc.perform(post("/api/v1/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    String registrationId = created.get("registrationId").asText();

    mockMvc.perform(delete("/api/v1/registrations/" + registrationId))
            .andExpect(status().isNoContent());

    // Confirm it's actually gone
    mockMvc.perform(get("/api/v1/registrations/" + registrationId))
            .andExpect(status().isNotFound());
}

@Test
void verifyQrSession_activeWithinTimeLimit() throws Exception {
    UUID eventId = UUID.randomUUID();

    String createResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/qr-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"validForMinutes\": 30}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    String qrCodeValue = created.get("qrCodeValue").asText();

    mockMvc.perform(get("/api/v1/qr-sessions/" + qrCodeValue + "/verify"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.eventId").value(eventId.toString()));
}

@Test
void verifyQrSession_inactiveAfterTimeLimitExpires() throws Exception {
    UUID eventId = UUID.randomUUID();

    String createResponse = mockMvc.perform(post("/api/v1/events/" + eventId + "/qr-sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"validForMinutes\": 30}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    String qrCodeValue = created.get("qrCodeValue").asText();

    // Force expiry directly in the database instead of waiting real time
    var session = qrSessionRepository.findByQrCodeValue(qrCodeValue).orElseThrow();
    session.setExpiresAt(Instant.now().minusSeconds(60));
    qrSessionRepository.save(session);

    mockMvc.perform(get("/api/v1/qr-sessions/" + qrCodeValue + "/verify"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
}
}