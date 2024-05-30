package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data @Getter @Setter @ToString
@Table(name = "ErrorResponse")
public class Error {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean status;
    private String details;
    private String message;
    private String solution;
    private String timestamp;
}
