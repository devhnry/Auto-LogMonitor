package org.remita.autologmonitor.service.impl;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.remita.autologmonitor.entity.LogError;
import org.remita.autologmonitor.enums.Status;
import org.remita.autologmonitor.repository.LogErrorRepository;
import org.remita.autologmonitor.service.EmailSenderService;
import org.remita.autologmonitor.service.LogErrorNotificationService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service @Slf4j @AllArgsConstructor
public class LogErrorNotificationServiceImpl implements LogErrorNotificationService {

    private static final String logDirectory = "log";
    private final EmailSenderService emailSenderService;
    private final LogErrorRepository logErrorRepository;

    @Override
    public void performErrorCheck() {
        loopThroughLogDirectory();
    }

    //todo - Create a Batch processing for multiple files
    private void loopThroughLogDirectory() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();
        Map<String, List<String>> map = new HashMap<>();

        if (directoryListing != null) {
            for (File logFile : directoryListing) {
                if(logFile.getName().endsWith(".log")){
                    executorService.submit(() -> {
                        try {
                            performAnalysisOnLines(logFile.getPath());
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

    private ArrayList<String> readChunkFromFile(String filePath) {
        File logFile = new File(filePath);
        log.info("Reading file: {}", logFile);
        ArrayList<String> allLogLines = new ArrayList<>();
        try (BufferedReader READER = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = READER.readLine()) != null) {
                allLogLines.add(line);
            }
        } catch (IOException e) {
            log.error("Exception occurred while reading file {}: {}", filePath, e.getMessage(), e);
        }
        log.info("Completed reading file: {}", filePath);
        return allLogLines;
    }

    private int binarySearchByTimestamp(String targetTimestamp, String filePath) {
        log.info("Performing binary search on LogFile {}", filePath);
        ArrayList<String> logs = readChunkFromFile(filePath);
        int left = 0, right = logs.size() - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            String midLine = logs.get(mid);
            String midTimestamp;

            // Checking if the line is not an error line
            if (!isTimestampLine(midLine)) {
                log.debug("Mid line is not a timestamp line: {}", midLine);
                while (mid < right && !isTimestampLine(logs.get(mid))) {
                    mid++;
                }
                if (mid >= right) {
                    log.debug("Reached end of logs while searching for timestamp line");
                    break;
                }
                midLine = logs.get(mid);
            }
            midTimestamp = extractTimestamp(midLine);
            log.debug("Comparing midTimestamp {} with targetTimestamp {}", midTimestamp, targetTimestamp);

            // The main binary search function
            if (midTimestamp != null) {
                if (midTimestamp.equals(targetTimestamp)) {
                    log.info("Timestamp match found at index {}", mid);
                    return mid;
                } else if (targetTimestamp.compareTo(midTimestamp) > 0) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            } else {
                log.debug("Mid timestamp is null, skipping this entry");
            }
        }
        log.info("No match found for timestamp {}", targetTimestamp);
        return -1;
    }

    private ArrayList<String> getNextChunkToAnalyse(String timeStamp, String filePath) {
        log.info("Getting next chunk to analyze from file: {}", filePath);
        ArrayList<String> logs = readChunkFromFile(filePath);
        int chunkSize = 50;
        int index = binarySearchByTimestamp(timeStamp, filePath);
        log.debug("Starting index for next chunk: {}", index);
        ArrayList<String> nextLogChunk = new ArrayList<>();
        for (int i = index + 1; i <= index + chunkSize && i < logs.size(); i++) {
            nextLogChunk.add(logs.get(i));
        }
        log.info("Next chunk to analyze contains {} lines", nextLogChunk.size());
        return nextLogChunk;
    }

    private void performAnalysisOnLines(String filePath) throws MessagingException {
        log.info("Performing Analysis check on LogFile {}", filePath);
        String timestamp = null;
        ArrayList<String> errorLines = new ArrayList<>();

        while (true) {
            ArrayList<String> logChunk = getNextChunkToAnalyse(timestamp, filePath);
            if (logChunk == null || logChunk.isEmpty()) {
                log.info("No more log chunks to analyze");
                break;
            }

            String currentTimestamp = null, errorTimesStamp = null;
            String lastTimestamp = null;

            for (String line : logChunk) {
                if (isTimestampLine(line)) {
                    currentTimestamp = extractTimestamp(line);
                    lastTimestamp = currentTimestamp;

                    if (line.contains("ERROR") || line.contains("WARN")) {
                        log.info("Error or Warn Detected on {} at timestamp {}", filePath, currentTimestamp);
                        errorTimesStamp = currentTimestamp;
                        errorLines.add(errorTimesStamp);
                    }
                }
            }
            timestamp = lastTimestamp;
            if (timestamp == null) {
                log.warn("Last timestamp is null, breaking out of the loop");
                break;
            }
        }
        saveErrorAsEntries(errorLines, filePath);
    }

    private void saveErrorAsEntries(ArrayList<String> errorLines, String filePath) throws MessagingException {
        log.info("Saving error lines for file: {}", filePath);
        ArrayList<String> logs = readChunkFromFile(filePath);
        Map<String, ArrayList<String>> logEntries = new HashMap<>();

        for (String errorLine : errorLines) {
            log.debug("Processing error line: {}", errorLine);
            boolean newTimeStampReached = false;
            int startIndex = binarySearchByTimestamp(errorLine, filePath);
            log.debug("Start index for error line {}: {}", errorLine, startIndex);

            ArrayList<String> logLines = new ArrayList<>();

            while (startIndex < logs.size() && !newTimeStampReached) {
                if (isTimestampLine(logs.get(startIndex))) {
                    newTimeStampReached = true;
                }
                logLines.add(logs.get(startIndex));
                startIndex++;
            }
            logEntries.put(errorLine, logLines);
        }
        sendMailToDevOps(logEntries, filePath);
    }

    private void saveErrorToDashboard(String msg, String timestamp) {
        LogError error = new LogError();
        error.setStatus(Status.PENDING);
        error.setMessage(msg);
        error.setTimeStamp(timestamp);
        error.setSolution("");

        log.info("Saving error to dashboard: {} at {}", msg, timestamp);
        logErrorRepository.save(error);
    }

    private void sendMailToDevOps(Map<String, ArrayList<String>> logEntries, String filename) throws MessagingException {
        log.info("Sending mail to devops for file: {}", filename);
        for (Map.Entry<String, ArrayList<String>> entry : logEntries.entrySet()) {
            MailResponseDto responseDto = new MailResponseDto();
            StringBuilder details = new StringBuilder();
            String message = String.format("%s  : Error occurred at Timestamp:  %s", filename.split("\\.")[0], extractTimeAndDate(entry.getKey()));
            responseDto.setTitle(message);
            responseDto.setEmail("taiwoh782@gmail.com");
            responseDto.setSubject("Error occurred on " + filename.split("\\.")[0]);

            for (String logLine : entry.getValue()) {
                details.append(logLine).append("\n");
            }
            responseDto.setBody("---------\n" + details.toString() + "\n---------");

            log.info("Sending email with subject: {}", responseDto.getSubject());
            emailSenderService.sendMail(responseDto);
            saveErrorToDashboard(message, extractTimeAndDate(entry.getKey()));
        }
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