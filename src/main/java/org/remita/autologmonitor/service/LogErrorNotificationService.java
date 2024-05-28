package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class LogErrorNotificationService {

    private static final String logDirectory = "log";
    private static EmailSenderService emailSenderService;
    private String baseUrl = "";

    public LogErrorNotificationService(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    public void performErrorCheck() {
        loopThroughLogDirectory();
    }

    private static void loopThroughLogDirectory() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File log : directoryListing) {
                executorService.submit(() -> {
                    try {
                        checkForError(
                                performLogCheckOnFile(String.format("%s/%s", logDirectory, log.getName())), log.getName());
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }

                });
            }
        } else {
            log.info("Error Reading Log File {}", logDirectory);
        }
        executorService.shutdown();
    }

    private static Map<String, List<String>> performLogCheckOnFile(String filePath) {
        File logFile = new File(filePath);

        Map<String, List<String>> logEntries = new HashMap<>();

        String currentTimestamp = null;
        List<String> currentLogLines = new ArrayList<>();



        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isTimestampLine(line)) {
                    // Update current timestamp
                    currentTimestamp = extractTimestamp(line);
                    // Store previous log lines
                    if (currentTimestamp != null) {
                        currentLogLines.add(line);
                        logEntries.put(currentTimestamp, new ArrayList<>(currentLogLines));
                        currentLogLines.clear();
                    }
                }

                //Add Empty space lines
                if(line.equals(""))
                    currentLogLines.add(line);

                // Add log line to current log lines (not timestamp line)
                currentLogLines.add(line);
            }

            // Add last log entry
            if (currentTimestamp != null) {
                logEntries.put(currentTimestamp, currentLogLines);
            }

        } catch (IOException e) {
            log.error("Error Reading Log File {}", logFile.getAbsolutePath());
            log.error("Error Reading Log File {}", e.getMessage());
        }
        return logEntries;
    }

    private static boolean isTimestampLine(String line) {
        return line.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}.*");
    }

    private static String extractTimestamp(String line) {
        return line.split(" ")[0];
    }

    private static void checkForError(Map<String, List<String>> logEntries, String filename) throws MessagingException {
        for (Map.Entry<String, List<String>> entry : logEntries.entrySet()) {
            MailResponseDto responseDto = new MailResponseDto();
            List<String> logs = entry.getValue();
            String details = null;
            for(String line : logs) {
                if(line.contains("ERROR") || line.contains("WARNING") || line.contains("EXCEPTION")){
                    responseDto.setTitle("Error occurred at Timestamp: " + extractTimestamp(line) + " on log file " + filename );
                    responseDto.setEmail("taiwo@systemspecscomng.onmicrosoft.com");
                    responseDto.setSubject("Attention: Error occurred on " + filename);
                    for (String logLine : entry.getValue()) {
                        details = logLine + "\n";
                    }
                    responseDto.setBody("\n --------- \n" + details + "\n --------- \n");

                    emailSenderService.sendMail(responseDto);
                }
            }
        }
    }
}
