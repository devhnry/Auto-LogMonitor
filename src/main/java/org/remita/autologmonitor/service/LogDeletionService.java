package org.remita.autologmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LogDeletionService {
    private static final String logDirectory = "log";

    public void deleteLogs(){
        System.out.println("Checking log directory: " + logDirectory);
        loopThroughLogDirectory();
    }

    private void loopThroughLogDirectory() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File log : directoryListing) {
                executorService.submit(() -> {
                    try {
                        performLogCheckOnFile("log/" + log.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } else {
            log.info("LogError Reading Log File");
        }
    }

    private void performLogCheckOnFile(String fileName) throws IOException{
        File logFile = new File(fileName);
        File tempFile = new File(UUID.randomUUID() + ".log");

        try (
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            String lastCurrentTimestamp = "";
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                List<String> logs = new ArrayList<>();
                logs.add(line);
                try {
                    String timeStamp =  "";
                    for(String log : logs){
                        if(isTimestampLine(log)){
                            timeStamp = extractTimestamp(log);
                            lastCurrentTimestamp = timeStamp;
                            if (!checkIfTimeStampIsOverdue(timeStamp) && !log.isEmpty() && !isErrorOrIrrelevant(log)) {
                                writer.write(log);
                                writer.newLine();
                            }
                        }else{
                            if (!checkIfTimeStampIsOverdue(lastCurrentTimestamp)) {
                                writer.write(log);
                                writer.newLine();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info("Timestamp format error: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.info("An error occurred while reading log file: {}", e.getMessage());
        }

        try {
            Files.delete(logFile.toPath());
            Files.move(tempFile.toPath(), logFile.toPath());
        } catch (IOException e) {
            log.info("An error occurred while replacing file: {}", e.getMessage());
        }
    }

    private static boolean checkIfTimeStampIsOverdue(String timeStamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate timeStampDate = LocalDate.parse(timeStamp, formatter);
        LocalDate currentDate = LocalDate.now();

        return timeStampDate.isBefore(currentDate.minusDays(7));
    }

    private static boolean isTimestampLine(String line) {
        return line.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\+\\d{2}:\\d{2}.*");
    }

    private static String extractTimestamp(String line) {
        return line.split(" ")[0].split("T")[0];
    }

    private boolean isErrorOrIrrelevant(String line) {
        return line.startsWith("***************************") ||
                line.startsWith("APPLICATION FAILED TO START") ||
                line.startsWith("Description") ||
                line.contains("Parameter") && line.contains("of constructor") && line.contains("required a bean") && line.contains("that could not be found") ||
                line.contains("org.") || line.contains("jakarta.") ||
                line.contains("java.base/") ||
                line.contains("Caused by") ||
                line.contains("common frames") || line.startsWith("LogError")
                || line.contains("Consider defining")
                || line.contains("Action");
    }
}
