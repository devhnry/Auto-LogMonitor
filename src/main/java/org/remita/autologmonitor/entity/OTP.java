package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter @ToString
@AllArgsConstructor @NoArgsConstructor
public class OTP {
    @Id
    @SequenceGenerator(
            name = "otp_sequence",
            sequenceName = "otp_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "otp_sequence"
    )
    private long id;
    private long otpCode;
    private LocalDate created_at;
    private LocalDate updated_at;
    private LocalDate expiration_time;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;
}
