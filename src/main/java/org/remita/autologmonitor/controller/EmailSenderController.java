package org.remita.autologmonitor.controller;

import dev.ditsche.mailo.MailAddress;
import dev.ditsche.mailo.factory.MailBuilder;
import jakarta.mail.MessagingException;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.remita.autologmonitor.service.EmailSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EmailSenderController {

    private final EmailSenderService emailSenderService;

    public EmailSenderController(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @PostMapping("/notify")
    public String sendMail(@RequestBody MailResponseDto mailResponseDto) throws MessagingException {
        emailSenderService.sendMail(mailResponseDto);
        return "Email Sent Successfully.!";
    }

}
