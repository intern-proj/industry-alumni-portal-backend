package com.nsbm.eventmanagementservice.dto;

import com.nsbm.eventmanagementservice.model.EventStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String eventType;
    private String coverImage;
    private String targetFaculties;
    private EventStatus status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long venueId;
    private Long organizationId;
    private Integer requiredAttendanceRate;
    private Long coordinatorUserId;
    private String coordinatorName;
    private String coordinatorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AgendaResponse> sessions;
}
