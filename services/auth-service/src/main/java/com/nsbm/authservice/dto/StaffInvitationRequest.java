package com.nsbm.authservice.dto;

import com.nsbm.authservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record StaffInvitationRequest(
        @NotNull(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        Role role
) {}