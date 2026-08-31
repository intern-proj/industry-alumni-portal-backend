package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmailDTO {

    @NotBlank(message = "Recipient email must not be blank")
    @Email(message = "Recipient email must be valid")
    private String toEmail;

    @NotBlank(message = "Recipient name must not be blank")
    private String recipientName;

    @NotNull(message = "Update type must not be null")
    private UpdateType updateType;

    @NotBlank(message = "Update body must not be blank")
    private String updateBody;

    private String actionLink;

    public enum UpdateType {
        PROFILE_APPROVED,
        JOB_POSTED,
        APPLICATION_UPDATE,
        GENERAL_UPDATE,
        VACANCY_APPROVED,
        VACANCY_CHANGES_REQUESTED,
        VACANCY_REJECTED
    }
}
