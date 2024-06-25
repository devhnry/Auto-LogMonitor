package org.remita.autologmonitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@AllArgsConstructor
@NoArgsConstructor
public class UserClaimsDto {
    private Long id;
    private String name;
    private String email;
}
