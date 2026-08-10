package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpEmailStatusDTO {

    @NotBlank(message = "To Email Cannot be Blank")
    String toEmail;

    @NotBlank(message = "Event Status Cannot Be Blank")
    Boolean status;

    String error;

}