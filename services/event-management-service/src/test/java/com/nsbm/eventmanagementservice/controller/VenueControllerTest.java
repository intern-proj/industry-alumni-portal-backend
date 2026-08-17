package com.nsbm.eventmanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.eventmanagementservice.dto.VenueRequest;
import com.nsbm.eventmanagementservice.dto.VenueResponse;
import com.nsbm.eventmanagementservice.exception.VenueNotFoundException;
import com.nsbm.eventmanagementservice.model.VenueType;
import com.nsbm.eventmanagementservice.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
@AutoConfigureMockMvc(addFilters = false)
public class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenueService venueService;

    private VenueResponse venueResponse;

    @BeforeEach
    void setUp() {
        venueResponse = VenueResponse.builder()
                .id(1L)
                .name("Auditorium A")
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();
    }

    @Test
    void createVenue_withValidRequest_returns201() throws Exception {
        VenueRequest request = VenueRequest.builder()
                .name("Auditorium A")
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();

        when(venueService.createVenue(any(VenueRequest.class))).thenReturn(venueResponse);

        mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Auditorium A"));
    }

    @Test
    void createVenue_withMissingName_returns400() throws Exception {
        VenueRequest request = VenueRequest.builder()
                .capacity(200)
                .venueType(VenueType.PHYSICAL)
                .build();

        mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllVenues_returns200WithList() throws Exception {
        when(venueService.getAllVenues()).thenReturn(List.of(venueResponse));

        mockMvc.perform(get("/api/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getVenueById_whenNotFound_returns404() throws Exception {
        when(venueService.getVenueById(999L)).thenThrow(new VenueNotFoundException(999L));

        mockMvc.perform(get("/api/venues/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateVenue_returns200() throws Exception {
        VenueRequest request = VenueRequest.builder()
                .name("Auditorium B")
                .capacity(300)
                .venueType(VenueType.HYBRID)
                .build();

        when(venueService.updateVenue(eq(1L), any(VenueRequest.class))).thenReturn(venueResponse);

        mockMvc.perform(put("/api/venues/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteVenue_returns204() throws Exception {
        doNothing().when(venueService).deleteVenue(1L);

        mockMvc.perform(delete("/api/venues/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}

