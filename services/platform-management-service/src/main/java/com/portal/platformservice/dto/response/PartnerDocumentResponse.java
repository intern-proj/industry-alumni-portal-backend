package com.portal.platformservice.dto.response;

import com.portal.platformservice.entity.DocumentType;
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
public class PartnerDocumentResponse {

    private UUID id;
    private DocumentType documentType;
    private UUID storageFileId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Instant uploadedAt;
    private boolean verified;
}
