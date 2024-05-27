package org.remita.autologmonitor.service;

import lombok.extern.slf4j.Slf4j;
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
                    checkForError(
                            performLogCheckOnFile(String.format("%s/%s", logDirectory, log.getName())), log.getName());

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

    private static void checkForError(Map<String, List<String>> logEntries, String filename) {
        for (Map.Entry<String, List<String>> entry : logEntries.entrySet()) {

            List<String> logs = entry.getValue();
            for(String line : logs) {
                if(line.contains("ERROR") || line.contains("WARNING") || line.contains("EXCEPTION")){
                    System.out.println("Error occurred at Timestamp: " + extractTimestamp(line) + " on log file " + filename );
                    System.out.println("\n#######Start#####\n");
                    for (String logLine : entry.getValue()) {
                        System.out.println(logLine);
                    }
                    System.out.println("\n#######End#####\n");

                }
            }
        }
    }
}
