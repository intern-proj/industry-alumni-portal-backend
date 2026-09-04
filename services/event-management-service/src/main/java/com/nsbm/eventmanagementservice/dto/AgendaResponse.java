package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaResponse {
    private Long id;
    private Long eventId;
    private String title;
    private String description;
    private List<LectureResponse> lectures;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sequenceOrder;
    private Long venueId;
    private String venueName;
    private Integer capacity;
    private String posterImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
