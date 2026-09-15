package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureResponse {
    private Long id;
    private Long agendaId;
    private String title;
    private String description;
    private Long speakerId;
    private String speakerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sequenceOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
