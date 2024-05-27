package org.remita.autologmonitor;

import org.remita.autologmonitor.service.EmailSenderService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoLogMonitorApplication {

    private EmailSenderService emailSenderService;

    public AutoLogMonitorApplication(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    public static void main(String[] args) {
        SpringApplication.run(AutoLogMonitorApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sendEmail(){
        emailSenderService.sendEmail("taiwoh782@gmail.com",
                "This is subject test", "This is av test");
    }

}
