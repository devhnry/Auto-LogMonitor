package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;

public interface LogDeletionService {
    void deleteLogs() throws MessagingException;
}
