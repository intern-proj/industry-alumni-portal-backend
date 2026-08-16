package com.nsbm.eventmanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.eventmanagementservice.dto.*;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.InvalidEventStatusTransitionException;
import com.nsbm.eventmanagementservice.model.EventStatus;
import com.nsbm.eventmanagementservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false) // disables Spring Security filters for this slice test
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        eventResponse = EventResponse.builder()
                .id(1L)
                .title("Industry Panel")
                .status(EventStatus.DRAFT)
                .startDateTime(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    void createEvent_withValidRequest_returns201() throws Exception {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Industry Panel")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .build();

        when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(eventResponse);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Industry Panel"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createEvent_withMissingTitle_returns400() throws Exception {
        CreateEventRequest request = CreateEventRequest.builder()
                .startDateTime(LocalDateTime.now().plusDays(5))
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());

        verify(eventService, never()).createEvent(any());
    }

    @Test
    void getEventById_whenExists_returns200() throws Exception {
        when(eventService.getEventById(1L)).thenReturn(eventResponse);

        mockMvc.perform(get("/api/events/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Industry Panel"));
    }

    @Test
    void getEventById_whenNotFound_returns404() throws Exception {
        when(eventService.getEventById(999L)).thenThrow(new EventNotFoundException(999L));

        mockMvc.perform(get("/api/events/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found with id: 999"));
    }

    @Test
    void getAllEvents_noFilters_returnsAllEvents() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of(eventResponse));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService).getAllEvents();
    }

    @Test
    void getAllEvents_withStatusFilter_callsFilteredMethod() throws Exception {
        when(eventService.getEventsByStatus(EventStatus.SCHEDULED)).thenReturn(List.of(eventResponse));

        mockMvc.perform(get("/api/events").param("status", "SCHEDULED"))
                .andExpect(status().isOk());

        verify(eventService).getEventsByStatus(EventStatus.SCHEDULED);
        verify(eventService, never()).getAllEvents();
    }

    @Test
    void updateStatus_validTransition_returns200() throws Exception {
        UpdateEventStatusRequest request = UpdateEventStatusRequest.builder()
                .status(EventStatus.SCHEDULED)
                .build();

        when(eventService.updateStatus(eq(1L), any(UpdateEventStatusRequest.class))).thenReturn(eventResponse);

        mockMvc.perform(patch("/api/events/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_invalidTransition_returns409() throws Exception {
        UpdateEventStatusRequest request = UpdateEventStatusRequest.builder()
                .status(EventStatus.COMPLETED)
                .build();

        when(eventService.updateStatus(eq(1L), any(UpdateEventStatusRequest.class)))
                .thenThrow(new InvalidEventStatusTransitionException(EventStatus.DRAFT, EventStatus.COMPLETED));

        mockMvc.perform(patch("/api/events/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void assignCoordinator_returns200WithCoordinatorDetails() throws Exception {
        AssignCoordinatorRequest request = AssignCoordinatorRequest.builder()
                .coordinatorUserId(42L)
                .coordinatorName("Ashini Fernando")
                .coordinatorEmail("ashini@example.com")
                .build();

        EventResponse withCoordinator = EventResponse.builder()
                .id(1L)
                .title("Industry Panel")
                .coordinatorUserId(42L)
                .coordinatorName("Ashini Fernando")
                .build();

        when(eventService.assignCoordinator(eq(1L), any(AssignCoordinatorRequest.class))).thenReturn(withCoordinator);

        mockMvc.perform(post("/api/events/{id}/coordinator", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordinatorName").value("Ashini Fernando"));
    }

    @Test
    void deleteEvent_returns204() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/events/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEvent(1L);
    }
}
