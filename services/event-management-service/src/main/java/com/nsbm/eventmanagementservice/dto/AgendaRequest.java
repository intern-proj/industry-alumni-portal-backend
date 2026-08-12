package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Long speakerId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer sequenceOrder;
}
