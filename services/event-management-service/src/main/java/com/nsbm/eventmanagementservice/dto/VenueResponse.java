package com.nsbm.eventmanagementservice.dto;

import com.nsbm.eventmanagementservice.model.VenueType;
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
    private VenueType venueType;
    private String contactInfo;
    private String onlineMeetingLink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
