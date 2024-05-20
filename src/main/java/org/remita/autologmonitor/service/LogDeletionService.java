package org.remita.autologmonitor.service;

import org.springframework.stereotype.Service;

@Service
public class LogDeletionService {
    private static String logDirectory = "log";

    public void deleteLogs() {
        System.out.println("Checking log directory: " + logDirectory);
    }
}
