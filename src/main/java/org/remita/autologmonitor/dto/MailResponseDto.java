package org.remita.autologmonitor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailResponseDto {
    private String subject;
    private String title;
    private String body;
    private String email;
}
