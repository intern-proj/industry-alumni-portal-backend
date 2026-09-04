package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String eventType;

    private String coverImage;

    private String targetFaculties;

    private List<AgendaRequest> sessions;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Long venueId;

    private Long organizationId;

    private Integer requiredAttendanceRate;
}
