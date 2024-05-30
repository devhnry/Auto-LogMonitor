package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data @Getter @Setter @ToString
@Table(name = "ErrorResponse")
public class Error {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
