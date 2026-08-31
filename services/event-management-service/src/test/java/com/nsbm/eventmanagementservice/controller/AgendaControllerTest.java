package com.nsbm.eventmanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.exception.AgendaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.GlobalExceptionHandler;
import com.nsbm.eventmanagementservice.service.AgendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AgendaControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AgendaService agendaService;

    @InjectMocks
    private AgendaController agendaController;

    private AgendaResponse agendaResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(agendaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        agendaResponse = AgendaResponse.builder()
                .id(1L)
                .eventId(1L)
                .speakerId(2L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    void createAgendaItem_withValidRequest_returns201() throws Exception {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .speakerId(2L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaService.createAgendaItem(any(AgendaRequest.class))).thenReturn(agendaResponse);

        mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Opening Keynote"));
    }

    @Test
    void createAgendaItem_withInvalidEvent_returns404() throws Exception {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(999L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaService.createAgendaItem(any(AgendaRequest.class)))
                .thenThrow(new EventNotFoundException(999L));

        mockMvc.perform(post("/api/v1/agendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAgendaItemById_whenNotFound_returns404() throws Exception {
        when(agendaService.getAgendaItemById(999L)).thenThrow(new AgendaNotFoundException(999L));

        mockMvc.perform(get("/api/v1/agendas/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAgenda_withEventId_returnsOrderedList() throws Exception {
        when(agendaService.getAgendaByEventId(1L)).thenReturn(List.of(agendaResponse));

        mockMvc.perform(get("/api/v1/agendas").param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(agendaService).getAgendaByEventId(1L);
    }

    @Test
    void getAgenda_withSpeakerId_returnsFilteredList() throws Exception {
        when(agendaService.getAgendaBySpeakerId(2L)).thenReturn(List.of(agendaResponse));

        mockMvc.perform(get("/api/v1/agendas").param("speakerId", "2"))
                .andExpect(status().isOk());

        verify(agendaService).getAgendaBySpeakerId(2L);
    }

    @Test
    void getAgenda_withNoParams_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/agendas"))
                .andExpect(status().isBadRequest());

        verify(agendaService, never()).getAgendaByEventId(any());
        verify(agendaService, never()).getAgendaBySpeakerId(any());
    }

    @Test
    void updateAgendaItem_returns200() throws Exception {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .title("Updated Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaService.updateAgendaItem(eq(1L), any(AgendaRequest.class))).thenReturn(agendaResponse);

        mockMvc.perform(put("/api/v1/agendas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAgendaItem_returns204() throws Exception {
        doNothing().when(agendaService).deleteAgendaItem(1L);

        mockMvc.perform(delete("/api/v1/agendas/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
