package com.portal.platformservice.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEmailDTO {

    private String toEmail;
    private String recipientName;
    private UpdateType updateType;
    private String updateBody;
    private String actionLink;

    public enum UpdateType {
        PROFILE_APPROVED,
        JOB_POSTED,
        APPLICATION_UPDATE,
        GENERAL_UPDATE
    }
}
