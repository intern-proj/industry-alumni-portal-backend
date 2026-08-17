package com.portal.platformservice.dto.request;

import com.portal.platformservice.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDocumentRegisterRequest {

    @NotNull
    private DocumentType documentType;

    /** Logical reference to the file object in Audit & Storage Service — no FK. */
    @NotNull
    private UUID storageFileId;

    private String originalFilename;

    private String contentType;

    private Long sizeBytes;
}
