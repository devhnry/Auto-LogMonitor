package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.remita.autologmonitor.enums.Roles;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter @Setter @ToString @AllArgsConstructor @NoArgsConstructor
public class User extends BaseUserEntity {
    @ManyToOne
    @JoinColumn(name = "organization_name")
    private Organization organization;

    @Column(updatable = false)
    private String createdBy;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(Roles.USER.name()));
    }
}
