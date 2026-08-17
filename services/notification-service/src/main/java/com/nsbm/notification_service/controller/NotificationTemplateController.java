package com.nsbm.notification_service.controller;

import com.nsbm.notification_service.dto.NotificationTemplateDTO;
import com.nsbm.notification_service.service.NotificationTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<NotificationTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @GetMapping("/code/{templateCode}")
    public ResponseEntity<NotificationTemplateDTO> getTemplateByCode(@PathVariable String templateCode) {
        return ResponseEntity.ok(templateService.getTemplateByCode(templateCode));
    }

    @PostMapping
    public ResponseEntity<NotificationTemplateDTO> createTemplate(@Valid @RequestBody NotificationTemplateDTO dto) {
        NotificationTemplateDTO created = templateService.createTemplate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateDTO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateDTO dto) {
        NotificationTemplateDTO updated = templateService.updateTemplate(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
