package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;
import org.remita.autologmonitor.dto.MailResponseDto;

public interface EmailSenderService {
    String sendMail(MailResponseDto mailResponseDto) throws MessagingException;
}
