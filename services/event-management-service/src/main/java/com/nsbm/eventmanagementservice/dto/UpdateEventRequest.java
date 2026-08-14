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

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private Long venueId;

    private Long organizationId;
}
