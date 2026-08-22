package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignCoordinatorRequest {
    @NotNull(message = "Coordinator user ID is required")
    private Long coordinatorUserId;

    @NotBlank(message = "Coordinator name is required")
    private String coordinatorName;

    @Email(message = "Must be a valid email")
    private String coordinatorEmail;
}
