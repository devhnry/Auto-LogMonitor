package org.remita.autologmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LogDeletionService {
    private static final String logDirectory = "log";

    public void deleteLogs() {
        System.out.println("Checking log directory: " + logDirectory);
        loopThroughLogDirectory();
    }

    public void loopThroughLogDirectory() {
        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File log : directoryListing) {
                performLogCheckOnFile("log/" + log.getName());
            }
        } else {
            log.info("Error Reading Log File");
        }
    }

    private void performLogCheckOnFile(String fileName){
        File logFile = new File(fileName);
        File tempFile = new File("tempLogFile.log");

        LocalDate currentDate = LocalDate.now();
        try (
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> logs = new ArrayList<>();
                logs.add(line);
                try {
                    String timeStamp =  "";
                    for(String log : logs){
                        if(log.split(":")[0].contains("T") && log.split("T").length > 1){
                            timeStamp = log.split("T")[0];
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

                        if (!(overdueDays >= 7) && !line.isEmpty()) {
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
}
