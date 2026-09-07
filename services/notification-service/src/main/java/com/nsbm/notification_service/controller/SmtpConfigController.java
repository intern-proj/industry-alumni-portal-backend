package com.nsbm.notification_service.controller;

import com.nsbm.notification_service.dto.SmtpConfigDTO;
import com.nsbm.notification_service.dto.SmtpTestRequestDTO;
import com.nsbm.notification_service.service.SmtpConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/notifications/smtp", "/api/v1/templates/smtp"})
@RequiredArgsConstructor
public class SmtpConfigController {

    private final SmtpConfigService smtpConfigService;

    @GetMapping
    public ResponseEntity<SmtpConfigDTO> getActiveSmtpConfig() {
        SmtpConfigDTO config = smtpConfigService.getActiveConfig();
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<SmtpConfigDTO> saveOrUpdateSmtpConfig(@Valid @RequestBody SmtpConfigDTO dto) {
        SmtpConfigDTO saved = smtpConfigService.saveOrUpdateConfig(dto);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSmtpConnection(@Valid @RequestBody SmtpTestRequestDTO testDto) {
        smtpConfigService.testSmtpConnection(testDto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test verification email dispatched successfully to " + testDto.getRecipientEmail()
        ));
    }
}
