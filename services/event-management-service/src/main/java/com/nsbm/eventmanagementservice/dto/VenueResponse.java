package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponse {
    private Long id;
    private String name;
    private String address;
    private Integer capacity;

    private String contactInfo;
    private String onlineMeetingLink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
