package com.nsbm.audit_storage_service.controller;

import com.nsbm.audit_storage_service.dto.StoredFileResponse;
import com.nsbm.audit_storage_service.model.FileType;
import com.nsbm.audit_storage_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") String uploaderId,
            @RequestParam(value = "fileType", defaultValue = "OTHER") FileType fileType
    ) {
        StoredFileResponse response = storageService.upload(file, uploaderId, fileType);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") UUID id) {
        StoredFileResponse metadata = storageService.getMetadata(id);
        Resource resource = storageService.download(id);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (metadata.getContentType() != null && !metadata.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(metadata.getContentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\""
                )
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoredFileResponse> getMetadata(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(storageService.getMetadata(id));
    }

    @GetMapping
    public ResponseEntity<List<StoredFileResponse>> listByUploader(
            @RequestParam("uploaderId") String uploaderId
    ) {
        return ResponseEntity.ok(storageService.listByUploader(uploaderId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        storageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
