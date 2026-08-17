package com.nsbm.audit_storage_service.controller;

import com.nsbm.audit_storage_service.dto.StoredFileResponse;
import com.nsbm.audit_storage_service.exception.GlobalExceptionHandler;
import com.nsbm.audit_storage_service.exception.ResourceNotFoundException;
import com.nsbm.audit_storage_service.model.FileType;
import com.nsbm.audit_storage_service.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageController.class)
@Import(GlobalExceptionHandler.class)
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    private UUID fileId;
    private StoredFileResponse storedFileResponse;

    @BeforeEach
    void setUp() {
        fileId = UUID.randomUUID();
        storedFileResponse = StoredFileResponse.builder()
                .fileId(fileId)
                .originalFilename("slides.pdf")
                .contentType("application/pdf")
                .fileSizeBytes(12L)
                .storageUrl("http://localhost:9000/icu-platform-files/slide/user-1/key_slides.pdf")
                .uploaderId("user-1")
                .uploadTimestamp(Instant.parse("2026-08-14T05:00:00Z"))
                .fileType(FileType.SLIDE)
                .version(1)
                .build();
    }

    @Test
    void upload_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "slides.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
        when(storageService.upload(any(MultipartFile.class), eq("user-1"), eq(FileType.SLIDE)))
                .thenReturn(storedFileResponse);

        mockMvc.perform(multipart("/api/storage/upload")
                        .file(file)
                        .param("uploaderId", "user-1")
                        .param("fileType", "SLIDE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.originalFilename").value("slides.pdf"))
                .andExpect(jsonPath("$.fileType").value("SLIDE"));

        verify(storageService, times(1))
                .upload(any(MultipartFile.class), eq("user-1"), eq(FileType.SLIDE));
    }

    @Test
    void download_returnsFileBytes() throws Exception {
        when(storageService.getMetadata(fileId)).thenReturn(storedFileResponse);
        when(storageService.download(fileId))
                .thenReturn(new ByteArrayResource("pdf-content".getBytes()));

        mockMvc.perform(get("/api/storage/download/{id}", fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"slides.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("pdf-content".getBytes()));

        verify(storageService, times(1)).getMetadata(fileId);
        verify(storageService, times(1)).download(fileId);
    }

    @Test
    void getMetadata_returnsOk() throws Exception {
        when(storageService.getMetadata(fileId)).thenReturn(storedFileResponse);

        mockMvc.perform(get("/api/storage/{id}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.uploaderId").value("user-1"));
    }

    @Test
    void getMetadata_returnsNotFound() throws Exception {
        when(storageService.getMetadata(fileId))
                .thenThrow(new ResourceNotFoundException("Stored file not found: " + fileId));

        mockMvc.perform(get("/api/storage/{id}", fileId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByUploader_returnsFiles() throws Exception {
        when(storageService.listByUploader("user-1")).thenReturn(List.of(storedFileResponse));

        mockMvc.perform(get("/api/storage").param("uploaderId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value(fileId.toString()))
                .andExpect(jsonPath("$[0].fileType").value("SLIDE"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(storageService).delete(fileId);

        mockMvc.perform(delete("/api/storage/{id}", fileId))
                .andExpect(status().isNoContent());

        verify(storageService, times(1)).delete(fileId);
    }

    @Test
    void delete_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Stored file not found: " + fileId))
                .when(storageService).delete(fileId);

        mockMvc.perform(delete("/api/storage/{id}", fileId))
                .andExpect(status().isNotFound());
    }
}
