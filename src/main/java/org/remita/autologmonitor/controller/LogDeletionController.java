package org.remita.autologmonitor.controller;

import jakarta.mail.MessagingException;
import org.remita.autologmonitor.service.LogDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LogDeletionController {

    private final LogDeletionService logDeletionService;
    public LogDeletionController(LogDeletionService logDeletionService) {
        this.logDeletionService = logDeletionService;
    }

    @Scheduled(cron = "0 0 * * * *")
    @GetMapping("/delete")
    public ResponseEntity<String> deleteLogsOlderThanSevenDays() throws MessagingException {
        logDeletionService.deleteLogs();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Application is running");
    }

}
