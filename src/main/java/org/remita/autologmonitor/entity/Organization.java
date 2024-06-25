package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.util.List;

@Entity
@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
@Table(name = "Organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private String id;
    private String organizationName;
    private String organizationDomain;
    private String organizationWebsite;

    @OneToMany(mappedBy = "organization")
    private List<Admin> userMembers;

    @OneToMany(mappedBy = "organization")
    private List<User> adminMembers;
}

