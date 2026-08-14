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
public class ReminderEmailDTO {

    @NotBlank(message = "Recipient email must not be blank")
    @Email(message = "Recipient email must be valid")
    private String toEmail;

    @NotBlank(message = "Recipient name must not be blank")
    private String recipientName;

    @NotNull(message = "Reminder type must not be null")
    private ReminderType reminderType;

    @NotBlank(message = "Reminder subject must not be blank")
    private String subject;

    @NotBlank(message = "Reminder body must not be blank")
    private String reminderBody;

    private String dueDate;

    private String actionLink;

    public enum ReminderType {
        EVENT,
        DEADLINE,
        GENERAL
    }
}
