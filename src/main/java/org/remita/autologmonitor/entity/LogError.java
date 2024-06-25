package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.remita.autologmonitor.enums.Status;

@Entity
@Getter @Setter @ToString
public class LogError {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String timeStamp;
    private String message;
    private Status status;
    private String solution;
}
