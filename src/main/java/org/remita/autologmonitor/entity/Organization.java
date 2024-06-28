package org.remita.autologmonitor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long organizationId;
    private String organizationName;
    private String organizationDomain;
    private String organizationWebsite;
    private String orgId;

//    @OneToMany(mappedBy = "organization")
//    private List<Admin> adminMembers;
//
//    @OneToMany(mappedBy = "organization")
//    private List<User> userMembers;
}

