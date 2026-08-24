package com.portal.platformservice.service;

import com.portal.platformservice.dto.request.PartnerDocumentRegisterRequest;
import com.portal.platformservice.dto.response.PartnerDocumentResponse;
import com.portal.platformservice.entity.PartnerDocument;
import com.portal.platformservice.entity.PartnerVerification;
import com.portal.platformservice.entity.VerificationStatus;
import com.portal.platformservice.exception.InvalidStateTransitionException;
import com.portal.platformservice.exception.ResourceNotFoundException;
import com.portal.platformservice.mapper.PartnerVerificationMapper;
import com.portal.platformservice.repository.PartnerDocumentRepository;
import com.portal.platformservice.repository.PartnerVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerDocumentService {

    private static final Set<VerificationStatus> DOCUMENT_UPLOAD_ALLOWED_STATUSES =
            EnumSet.of(VerificationStatus.PENDING_DOCUMENTS, VerificationStatus.MORE_INFO_REQUIRED);

    private final PartnerVerificationRepository verificationRepository;
    private final PartnerDocumentRepository documentRepository;

    @Transactional
    public PartnerDocumentResponse addDocument(UUID verificationId, PartnerDocumentRegisterRequest request) {
        PartnerVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner verification not found: " + verificationId));

        if (!DOCUMENT_UPLOAD_ALLOWED_STATUSES.contains(verification.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Cannot add documents to a verification in status " + verification.getStatus());
        }

        PartnerDocument document = PartnerDocument.builder()
                .documentType(request.getDocumentType())
                .storageFileId(request.getStorageFileId())
                .originalFilename(request.getOriginalFilename())
                .contentType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .build();

        verification.addDocument(document);
        // Persisted through the owning side directly, not via cascade off
        // verification.save() — cascade-through-parent only flushes (and
        // assigns the generated id / uploadedAt) at transaction commit,
        // which is too late for the id we return below.
        documentRepository.save(document);

        return PartnerVerificationMapper.toDocumentResponse(document);
    }

    @Transactional(readOnly = true)
    public List<PartnerDocumentResponse> listDocuments(UUID verificationId) {
        return documentRepository.findByVerification_Id(verificationId).stream()
                .map(PartnerVerificationMapper::toDocumentResponse)
                .toList();
    }

    @Transactional
    public void deleteDocument(UUID verificationId, UUID documentId) {
        PartnerDocument document = documentRepository.findById(documentId)
                .filter(doc -> doc.getVerification().getId().equals(verificationId))
                .orElseThrow(() -> new ResourceNotFoundException("Partner document not found: " + documentId));

        if (!DOCUMENT_UPLOAD_ALLOWED_STATUSES.contains(document.getVerification().getStatus())) {
            throw new InvalidStateTransitionException(
                    "Cannot remove documents from a verification in status " + document.getVerification().getStatus());
        }

        documentRepository.delete(document);
    }
}
