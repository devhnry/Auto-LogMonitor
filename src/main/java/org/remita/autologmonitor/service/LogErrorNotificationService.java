package org.remita.autologmonitor.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class LogErrorNotificationService {

    public void performLogCheckOnFile(){
        File logFile = new File("log/service.log");

        try (
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> logs = new ArrayList<>();
                logs.add(line);

                try {
                    String timeStamp =  "";
                    for(String log : logs){
                        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                        String timestamp = log.split(" ")[0].split("T")[0];
                        Matcher matching = pattern.matcher(timestamp);

                        if(matching.matches()){
                            timestamp = timeStamp;
                        }

                        String information = log.split(" ")[0] + log.split(" ")[1];
                        System.out.println(information);
                    }

                } catch (Exception e) {
                    System.out.println("Timestamp format error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
