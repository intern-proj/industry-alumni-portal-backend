package com.nsbm.eventmanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.exception.GuestSpeakerNotFoundException;
import com.nsbm.eventmanagementservice.service.GuestSpeakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestSpeakerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GuestSpeakerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuestSpeakerService guestSpeakerService;

    private GuestSpeakerResponse speakerResponse;

    @BeforeEach
    void setUp() {
        speakerResponse = GuestSpeakerResponse.builder()
                .id(1L)
                .fullName("Dr. Nimal Perera")
                .email("nimal.perera@example.com")
                .organizationId(5L)
                .build();
    }

    @Test
    void createSpeaker_withValidRequest_returns201() throws Exception {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder()
                .fullName("Dr. Nimal Perera")
                .email("nimal.perera@example.com")
                .organizationId(5L)
                .build();

        when(guestSpeakerService.createSpeaker(any(GuestSpeakerRequest.class))).thenReturn(speakerResponse);

        mockMvc.perform(post("/api/guest-speakers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Dr. Nimal Perera"));
    }

    @Test
    void createSpeaker_withMissingFullName_returns400() throws Exception {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder()
                .email("nimal.perera@example.com")
                .build();

        mockMvc.perform(post("/api/guest-speakers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSpeakerById_whenExists_returns200() throws Exception {
        when(guestSpeakerService.getSpeakerById(1L)).thenReturn(speakerResponse);

        mockMvc.perform(get("/api/guest-speakers/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getSpeakerById_whenNotFound_returns404() throws Exception {
        when(guestSpeakerService.getSpeakerById(999L)).thenThrow(new GuestSpeakerNotFoundException(999L));

        mockMvc.perform(get("/api/guest-speakers/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllSpeakers_noFilter_returnsAll() throws Exception {
        when(guestSpeakerService.getAllSpeakers()).thenReturn(List.of(speakerResponse));

        mockMvc.perform(get("/api/guest-speakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(guestSpeakerService).getAllSpeakers();
    }

    @Test
    void getAllSpeakers_withOrganizationFilter_returnsFiltered() throws Exception {
        when(guestSpeakerService.getSpeakersByOrganization(5L)).thenReturn(List.of(speakerResponse));

        mockMvc.perform(get("/api/guest-speakers").param("organizationId", "5"))
                .andExpect(status().isOk());

        verify(guestSpeakerService).getSpeakersByOrganization(5L);
        verify(guestSpeakerService, never()).getAllSpeakers();
    }

    @Test
    void updateSpeaker_returns200() throws Exception {
        GuestSpeakerRequest request = GuestSpeakerRequest.builder()
                .fullName("Dr. Nimal Perera Jr.")
                .build();

        when(guestSpeakerService.updateSpeaker(eq(1L), any(GuestSpeakerRequest.class))).thenReturn(speakerResponse);

        mockMvc.perform(put("/api/guest-speakers/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSpeaker_returns204() throws Exception {
        doNothing().when(guestSpeakerService).deleteSpeaker(1L);

        mockMvc.perform(delete("/api/guest-speakers/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
