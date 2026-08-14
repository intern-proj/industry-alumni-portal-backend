package com.nsbm.notification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.notification_service.dto.NotificationTemplateDTO;
import com.nsbm.notification_service.service.NotificationTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationTemplateService templateService;

    @InjectMocks
    private NotificationTemplateController templateController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(templateController).build();
    }

    @Test
    void test_getAllTemplates() throws Exception {
        NotificationTemplateDTO dto = NotificationTemplateDTO.builder()
                .id(1L)
                .templateCode("ANNOUNCEMENT_EMAIL")
                .name("Announcement")
                .subject("Title")
                .body("<p>Body</p>")
                .build();

        when(templateService.getAllTemplates()).thenReturn(List.of(dto));

        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateCode").value("ANNOUNCEMENT_EMAIL"));
    }

    @Test
    void test_createTemplate() throws Exception {
        NotificationTemplateDTO input = NotificationTemplateDTO.builder()
                .templateCode("ANNOUNCEMENT_EMAIL")
                .name("Announcement")
                .subject("Title")
                .body("<p>Body</p>")
                .build();

        NotificationTemplateDTO output = NotificationTemplateDTO.builder()
                .id(1L)
                .templateCode("ANNOUNCEMENT_EMAIL")
                .name("Announcement")
                .subject("Title")
                .body("<p>Body</p>")
                .build();

        when(templateService.createTemplate(any())).thenReturn(output);

        mockMvc.perform(post("/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.templateCode").value("ANNOUNCEMENT_EMAIL"));
    }

    @Test
    void test_deleteTemplate() throws Exception {
        mockMvc.perform(delete("/templates/1"))
                .andExpect(status().isNoContent());
    }
}
