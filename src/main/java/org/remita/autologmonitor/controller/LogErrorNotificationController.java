package org.remita.autologmonitor.controller;

import org.remita.autologmonitor.service.LogErrorNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LogErrorNotificationController {

    public final LogErrorNotificationService logErrorNotificationService;

    public LogErrorNotificationController(LogErrorNotificationService logErrorNotificationService) {
        this.logErrorNotificationService = logErrorNotificationService;
    }

    @GetMapping("/errorCheck")
    public ResponseEntity<String> errorCheck() {
        logErrorNotificationService.performLogCheckOnFile();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Application for Error check is working");
    }
}
