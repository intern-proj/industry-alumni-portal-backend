package com.nsbm.audit_storage_service.dto;

import com.nsbm.audit_storage_service.model.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFileResponse {

    private UUID fileId;
    private String originalFilename;
    private String contentType;
    private Long fileSizeBytes;
    private String storageUrl;
    private String downloadUrl;
    private String uploaderId;
    private Instant uploadTimestamp;
    private FileType fileType;
    private Integer version;
}
