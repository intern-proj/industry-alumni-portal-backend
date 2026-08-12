package com.portal.certificateservice.controller;

import com.portal.certificateservice.dto.CreateTemplateRequestDto;
import com.portal.certificateservice.dto.TemplateResponseDto;
import com.portal.certificateservice.dto.UpdateTemplateRequestDto;
import com.portal.certificateservice.service.CertificateTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificate-templates")
public class CertificateTemplateController {

    private final CertificateTemplateService templateService;

    public CertificateTemplateController(CertificateTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponseDto> createTemplate(@Valid @RequestBody CreateTemplateRequestDto request) {
        TemplateResponseDto created = templateService.createTemplate(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponseDto>> getAllTemplates(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<TemplateResponseDto> templates = activeOnly ? templateService.getActiveTemplates() : templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponseDto> getTemplateById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponseDto> updateTemplate(@PathVariable UUID id, @RequestBody UpdateTemplateRequestDto request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
