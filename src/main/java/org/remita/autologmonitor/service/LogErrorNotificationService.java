package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.remita.autologmonitor.entity.LogError;
import org.remita.autologmonitor.entity.Status;
import org.remita.autologmonitor.repository.LogErrorRepository;
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

@Service @Slf4j
@AllArgsConstructor
public class LogErrorNotificationService {

    private static final String logDirectory = "log";
    private final EmailSenderService emailSenderService;
    private final LogErrorRepository logErrorRepository;

    public void performErrorCheck() {
        loopThroughLogDirectory();
    }

    private void loopThroughLogDirectory() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            for (File logFile : directoryListing) {
                if(logFile.getName().endsWith(".log")){
                    executorService.submit(() -> {
                        try {
                            sendMailToDevOps(performErrorCheckOnFile(String.format("%s/%s", logDirectory, logFile.getName())), logFile.getName());
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }

                    });
                }
            }
        } else {
            log.info("Error Reading Log File {}", logDirectory);
        }
        executorService.shutdown();
    }

    private void saveErrorToDashboard(String msg, String details, String timestamp){
        LogError error = new LogError();
        error.setStatus(Status.PENDING);
        error.setMessage(msg);
        error.setDetails(details);
        error.setTimeStamp(timestamp);
        error.setSolution("");

        logErrorRepository.save(error);
    }

    private void sendMailToDevOps(Map<String, List<String>> logEntries, String filename) throws MessagingException {
        for (Map.Entry<String, List<String>> entry : logEntries.entrySet()) {
            MailResponseDto responseDto = new MailResponseDto();
            String details = null;
            String message = String.format("%s  : Error occurred at Timestamp:  %s",  filename.split("\\.")[0], extractTimeAndDate(entry.getKey()));
            responseDto.setTitle(message);
            responseDto.setEmail("taiwoh782@gmail.com");
            responseDto.setSubject("Error occurred on " + filename.split("\\.")[0]);

            for (String logLines : entry.getValue()){
                details += logLines + "\n";
            }
            responseDto.setBody("---------\n" + details + "\n---------");

            emailSenderService.sendMail(responseDto);
            saveErrorToDashboard(message, details, extractTimeAndDate(entry.getKey()));
        }
    }

    private static Map<String, List<String>> performErrorCheckOnFile(String filePath) {
        File logFile = new File(filePath);
        Map<String, List<String>> logEntries = new HashMap<>();
        String currentTimestamp = null;
        String errorTimesStamp = null;
        List<String> currentLogLines = new ArrayList<>();
        Boolean errorFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isTimestampLine(line)) {
                    // Update current timestamp and error
                    currentTimestamp = extractTimestamp(line);
                    errorFound = false;

                    // Check for Error
                    if(line.contains("ERROR") || line.contains("WARN")){
                        errorFound = true;
                        errorTimesStamp = currentTimestamp;
                    }
                }

                // Save Info Related to Error
                if(errorFound) {
                    currentLogLines.add(line);
                } else {
                    if(errorTimesStamp == null && currentLogLines.size() == 0) continue;
                    logEntries.put(errorTimesStamp, new ArrayList<>(currentLogLines));
                    currentLogLines = new ArrayList<>();
                    errorTimesStamp = null;
                }
            }
        } catch (IOException e) {
            log.error("LogError Reading Log File {}", e.getMessage());
        }
        return logEntries;
    }

    private static boolean isTimestampLine(String line) {
        return line.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}.*");
    }

    private static String extractTimestamp(String line) {
        return line.split(" ")[0];
    }

    private static String extractTimeAndDate(String line) {
        return line.split(" ")[0].split("\\.")[0];
    }
}