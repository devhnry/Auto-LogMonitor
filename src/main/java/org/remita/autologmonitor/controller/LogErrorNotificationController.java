package org.remita.autologmonitor.controller;

import org.remita.autologmonitor.service.impl.LogErrorNotificationServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LogErrorNotificationController {

    public final LogErrorNotificationServiceImpl logErrorNotificationService;

    public LogErrorNotificationController(LogErrorNotificationServiceImpl logErrorNotificationService) {
        this.logErrorNotificationService = logErrorNotificationService;
    }

//    @Scheduled(cron = "0 0 * * * *")
    @GetMapping("/errorCheck")
    public ResponseEntity<String> errorCheck() {
        logErrorNotificationService.performErrorCheck();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Application for LogError check is working");
    }
}
