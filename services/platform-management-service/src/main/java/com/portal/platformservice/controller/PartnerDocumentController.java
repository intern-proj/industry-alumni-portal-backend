package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.PartnerDocumentRegisterRequest;
import com.portal.platformservice.dto.response.PartnerDocumentResponse;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.service.PartnerDocumentService;
import com.portal.platformservice.service.PartnerVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partner-verifications/{verificationId}")
@RequiredArgsConstructor
public class PartnerDocumentController {

    private final PartnerDocumentService partnerDocumentService;
    private final PartnerVerificationService partnerVerificationService;

    @PostMapping("/documents")
    public ResponseEntity<PartnerDocumentResponse> addDocument(
            @PathVariable UUID verificationId,
            @Valid @RequestBody PartnerDocumentRegisterRequest request) {
        PartnerDocumentResponse response = partnerDocumentService.addDocument(verificationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents")
    public List<PartnerDocumentResponse> listDocuments(@PathVariable UUID verificationId) {
        return partnerDocumentService.listDocuments(verificationId);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID verificationId, @PathVariable UUID documentId) {
        partnerDocumentService.deleteDocument(verificationId, documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit-for-review")
    public PartnerVerificationResponse submitForReview(@PathVariable UUID verificationId) {
        return partnerVerificationService.submitForReview(verificationId);
    }
}
