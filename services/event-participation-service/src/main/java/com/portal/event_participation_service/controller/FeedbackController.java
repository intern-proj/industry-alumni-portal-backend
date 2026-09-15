package com.portal.event_participation_service.controller;

import com.portal.event_participation_service.dto.FeedbackRequest;
import com.portal.event_participation_service.dto.FeedbackResponse;
import com.portal.event_participation_service.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.submit(request);
        return ResponseEntity
                .created(URI.create("/api/v1/feedback/" + response.feedbackId()))
                .body(response);
    }
}
