package org.remita.autologmonitor.service;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.remita.autologmonitor.dto.MailResponseDto;
import org.remita.autologmonitor.entity.LogError;
import org.remita.autologmonitor.entity.Status;
import org.remita.autologmonitor.repository.LogErrorRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service @Slf4j @AllArgsConstructor
public class LogErrorNotificationService {

    private static final String logDirectory = "log";
    private final EmailSenderService emailSenderService;
    private final LogErrorRepository logErrorRepository;

    public void performErrorCheck() {
        loopThroughLogDirectory();
    }

    //todo - Create a Batch processing for multiple files
    private void loopThroughLogDirectory() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        File dir = new File(logDirectory);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            for (File logFile : directoryListing) {
                if(logFile.getName().endsWith(".log")){
                    executorService.submit(() -> {
                        try {
                            sendMailToDevOps(
//                                    performErrorCheckOnFile(
//                                            String.format("%s/%s", logDirectory, logFile.getName())), logFile.getName());
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

    private static ArrayList<String> readChunkFromFile(String filePath){
        File logFile = new File(filePath);
        ArrayList<String> allLogLines = new ArrayList<>();
        try(BufferedReader READER = new BufferedReader(new FileReader(logFile))){
            String line;
            while ((line = READER.readLine()) != null){
                allLogLines.add(line);
            }
        }catch (IOException e){
            log.error("Exception Occurred while reading file {}", e.getMessage());
        }
        return allLogLines;
    }

    private void saveErrorToDashboard(String msg, String timestamp){
        LogError error = new LogError();
        error.setStatus(Status.PENDING);
        error.setMessage(msg);
        error.setTimeStamp(timestamp);
        error.setSolution("");

        logErrorRepository.save(error);
    }

    private void sendMailToDevOps(Map<String, List<String>> logEntries, String filename) throws MessagingException {
        for (Map.Entry<String, List<String>> entry : logEntries.entrySet()) {
            MailResponseDto responseDto = new MailResponseDto();
            StringBuilder details = null;
            String message = String.format("%s  : Error occurred at Timestamp:  %s",  filename.split("\\.")[0], extractTimeAndDate(entry.getKey()));
            responseDto.setTitle(message);
            responseDto.setEmail("taiwoh782@gmail.com");
            responseDto.setSubject("Error occurred on " + filename.split("\\.")[0]);

            for (String logLines : entry.getValue()){
                details.append(logLines).append("\n");
            }
            responseDto.setBody("---------\n" + details + "\n---------");

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