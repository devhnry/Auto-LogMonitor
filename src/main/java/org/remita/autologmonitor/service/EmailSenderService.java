package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailSenderService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public EmailSenderService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    public String sendMail(MailResponseDto mailResponseDto) throws MessagingException {
        Context context = new Context();
        context.setVariable("response", mailResponseDto);

        String process = templateEngine.process("LogError", context);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setSubject("Attention: " + mailResponseDto.getSubject() );
        helper.setText(process, true);
        helper.setTo(new String[]{mailResponseDto.getEmail(), "taiwoh782@gmail.com"});
        javaMailSender.send(mimeMessage);
        return "Sent";
    }
}
