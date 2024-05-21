package org.remita.autologmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
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
    private static String timeDate = "";

    public void deleteLogs() throws IOException {
        System.out.println("Checking log directory: " + logDirectory);
        loopThroughLogDirectory();
    }

    private void loopThroughLogDirectory() throws IOException{
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
            log.info("Error Reading Log File");
        }
    }

    private void performLogCheckOnFile(String fileName) throws IOException{


        File logFile = new File(fileName);
        File tempFile = new File(UUID.randomUUID() + ".log");

        boolean success = tempFile.createNewFile();
        System.out.println("Success: " + success);

        LocalDate currentDate = LocalDate.now();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {

            String initialLine = reader.readLine();
            if(initialLine != null) {
                String log1 = initialLine.split(" ")[0].split("\\.")[0];

                Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                Matcher m = pattern.matcher(log1);
                log.info("The timestamp {} matching the pattern is + {}", log1, m);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> logs = new ArrayList<>();
                logs.add(line);

                try {
                    String timeStamp =  "";
                    for(String log : logs){
                        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                        timeDate = log.split(" ")[0].split("T")[0];
                        Matcher matching = pattern.matcher(timeDate);

                        if(matching.matches()){
                            timeStamp = timeDate;
                        }

                        int year, month, day;
                        if(timeStamp.isEmpty()){
                            continue;
                        }else{
                            year = Integer.parseInt(timeStamp.split("-")[0]);
                            month = Integer.parseInt(timeStamp.split("-")[1]);
                            day = Integer.parseInt(timeStamp.split("-")[2]);
                        }
                        int overdueDays = currentDate.getDayOfMonth() - LocalDate.of(year, month, day).getDayOfMonth();

                        if (!(overdueDays >= 7) && !line.isEmpty() && !isErrorOrIrrelevant(line)) {
                            writer.write(log);
                            writer.newLine();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Timestamp format error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.delete(logFile.toPath());
            Files.move(tempFile.toPath(), logFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isErrorOrIrrelevant(String line) {
        return line.startsWith("***************************") ||
                line.startsWith("APPLICATION FAILED TO START") ||
                line.startsWith("Description") ||
                line.contains("Parameter") && line.contains("of constructor") && line.contains("required a bean") && line.contains("that could not be found") ||
                line.contains("org.") || line.contains("jakarta.") ||
                line.contains("java.base/") ||
                line.contains("Caused by") ||
                line.contains("common frames") || line.startsWith("Error")
                || line.contains("Consider defining")
                || line.contains("Action");
    }
}
