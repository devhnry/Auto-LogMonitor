package org.remita.autologmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.remita.autologmonitor.enums.TokenType;

@Entity
@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
@Table(name = "Tokens")
public class Token {

    @Id
    @GeneratedValue
    private Long id;
    private String token;
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;
    private Boolean expired;
    private Boolean revoked;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "userId")
    private User users;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "adminId")
    private Admin admin;

    @Override
    public String toString(){
        return "Token: " + token;
    }

}
