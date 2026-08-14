package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpEmailDTO {

    @NotBlank(message = "Email should not be blank")
    @Email(message = "Email should be valid")
    String toEmail;

    @NotBlank(message = "OTP Code must not be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP Code should be 6 digits")
    String otpCode;
}
