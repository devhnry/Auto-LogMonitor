package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.remita.autologmonitor.enums.Roles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter @Setter @ToString
public class Admin extends BaseUserEntity implements IBaseUserEntity{
    @Enumerated(EnumType.STRING)
    private Roles role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(Roles.ADMIN.name()));
    }
}
