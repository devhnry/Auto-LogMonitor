package org.remita.autologmonitor.controller;

import org.remita.autologmonitor.service.LogDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/delete")
    public ResponseEntity<String> deleteLogsOlderThanSevenDays(){
        logDeletionService.deleteLogs();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Application is running");
    }

}
