package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data @Getter @Setter @ToString
@Table(name = "ErrorResponse")
public class LogError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean status;
    private String details;
    private String message;
    private String solution;
    private String timestamp;
}
