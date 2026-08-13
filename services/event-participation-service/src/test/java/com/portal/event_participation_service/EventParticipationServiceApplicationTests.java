package com.portal.event_participation_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.event_participation_service.dto.RegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventParticipationServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
}