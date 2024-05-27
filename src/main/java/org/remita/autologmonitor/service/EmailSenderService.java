package org.remita.autologmonitor.service;

import dev.ditsche.mailo.factory.MailBuilder;
import dev.ditsche.mailo.provider.MailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailSenderService {

    private final MailProvider mailProvider;

    public EmailSenderService(MailProvider mailProvider) {
        this.mailProvider = mailProvider;
    }

    @Async
    public void sendEmail(MailBuilder mailBuilder) {
        if(mailProvider.send(mailBuilder))
            log.info("Email sent successfully");
        else
            log.error("Error sending email..");
    }

}
