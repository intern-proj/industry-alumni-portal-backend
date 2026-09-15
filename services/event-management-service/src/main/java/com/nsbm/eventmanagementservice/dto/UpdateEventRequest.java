package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {
    private String title;

    private String description;

    private String eventType;
    private String coverImage;
    private String targetFaculties;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Long venueId;

    private Long organizationId;
}
