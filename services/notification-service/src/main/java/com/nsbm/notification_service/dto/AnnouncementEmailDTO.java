package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementEmailDTO {

    @NotEmpty(message = "Recipient email list must not be empty")
    private List<@Email(message = "Each recipient email must be valid") String> toEmails;

    @NotBlank(message = "Announcement title must not be blank")
    private String announcementTitle;

    @NotBlank(message = "Announcement body must not be blank")
    private String announcementBody;

    @NotBlank(message = "Sender name must not be blank")
    private String senderName;

    private String portalUrl;
}
