package org.remita.autologmonitor;

import org.remita.autologmonitor.repository.LogErrorRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoLogMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutoLogMonitorApplication.class, args);
    }
}
