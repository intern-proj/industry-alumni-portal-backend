package com.nsbm.eventmanagementservice.dto;

import com.nsbm.eventmanagementservice.model.EventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventStatusRequest {
    @NotNull(message = "Status is required")
    private EventStatus status;
}
