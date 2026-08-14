package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleEventRequest {
    @NotNull(message = "New start date/time is required")
    @Future(message = "New start date/time must be in the future")
    private LocalDateTime newStartDateTime;

    private LocalDateTime newEndDateTime;

    private String reason;
}
