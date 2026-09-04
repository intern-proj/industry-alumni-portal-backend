package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaRequest {
    private Long eventId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private List<LectureRequest> lectures;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer sequenceOrder;

    private Long venueId;

    private Integer capacity;

    private String posterImage;
}
