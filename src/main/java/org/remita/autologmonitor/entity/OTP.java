package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

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
    private Long id;
    private String otpCode;
    private Date createdAt;
    private Date updatedAt;
    private Date expirationTime;
    private boolean isRevoked;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "adminId")
    private Admin admin;
}
