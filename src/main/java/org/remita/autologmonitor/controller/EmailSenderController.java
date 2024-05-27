package org.remita.autologmonitor.controller;

import dev.ditsche.mailo.MailAddress;
import dev.ditsche.mailo.factory.MailBuilder;
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
    public ResponseEntity<?> notifyDevops(@RequestBody MailResponseDto mailResponseDto) {
        MailBuilder mailBuilder = MailBuilder.mjml()
                .subject(mailResponseDto.getSubject())
                .to(new MailAddress(mailResponseDto.getEmail()))
                .from(new MailAddress("devwhenry@gmail.com"))
                .param("email", mailResponseDto.getEmail())
                .param("body", mailResponseDto.getBody())
                .loadTemplate("notificationMail");

        emailSenderService.sendEmail(mailBuilder);
        return ResponseEntity.ok("Queued");
    }

}
