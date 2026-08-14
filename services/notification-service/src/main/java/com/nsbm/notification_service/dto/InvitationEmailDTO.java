package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitationEmailDTO {

    @NotBlank(message = "Recipient email must not be blank")
    @Email(message = "Recipient email must be valid")
    private String toEmail;

    @NotBlank(message = "Invitee name must not be blank")
    private String inviteeName;

    @NotBlank(message = "Event name must not be blank")
    private String eventName;

    @NotBlank(message = "Event date must not be blank")
    private String eventDate;

    private String eventLocation;

    private String eventDescription;

    private String rsvpLink;

    private String organizerName;
}
