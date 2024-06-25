package org.remita.autologmonitor.service.impl;

import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.jvnet.hk2.annotations.Service;
import org.remita.autologmonitor.dto.DefaultResponseDto;
import org.remita.autologmonitor.dto.SignupRequestDto;
import org.remita.autologmonitor.entity.Admin;
import org.remita.autologmonitor.entity.Business;
import org.remita.autologmonitor.entity.Organization;
import org.remita.autologmonitor.enums.Roles;
import org.remita.autologmonitor.repository.*;
import org.remita.autologmonitor.service.AuthenticationService;
import org.remita.autologmonitor.service.JWTService;
import org.springframework.security.authentication.AuthenticationManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AdminRepository adminRepository;
    private final BusinessRepository businessRepository;
    private final OrganizationRepository organizationRepository;

    private DefaultResponseDto signup(SignupRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        Organization organization = new Organization();
        Admin admin = new Admin();
        Business newBusiness = new Business();

        SignupRequestDto request = req;
        List<String> emptyProperties = hasNoNullProperties(request);
        if(!emptyProperties.isEmpty()) {
            StringBuilder data = new StringBuilder();
            for (String property : emptyProperties) {
                data.append(String.format("{} has not been filled \n", property));
            }
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("Invalid Request Made");
            res.setData(data);
            return res;
        }

        if(!req.getPassword().equals(req.getConfirmPassword())){
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("Invalid Confirm Password");
            res.setData("Passwords do not match");
        }

        assignRequestToEntity(req, organization, admin, newBusiness);
        return res;
    };

    private DefaultResponseDto login(DefaultResponseDto responseDTO) {
        DefaultResponseDto res = new DefaultResponseDto();

        return res;
    };

    private List<String> hasNoNullProperties(Object entity) {
        List<String> list = new ArrayList<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.get(entity) == null) {
                    list.add(field.getName());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void assignRequestToEntity(SignupRequestDto req, Organization organization, Admin admin, Business newBusiness){
        organization = new Organization();
        admin = new Admin();
        newBusiness = new Business();

        String email = req.getEmail();
        String password = req.getPassword();
        String confirmPassword = req.getConfirmPassword();
        String firstName = req.getFirstName();
        String lastName = req.getLastName();
        String phoneNumber = req.getPhoneNumber();
        String organizationName = req.getOrganizationName();
        String organizationEmail = req.getOrganizationEmail();
        String foundUsBy = req.getFoundUsBy();

        newBusiness.setEmail(email);
        newBusiness.setFirstName(firstName);
        newBusiness.setLastName(lastName);
        newBusiness.setPassword(password);
        newBusiness.setPhoneNumber(phoneNumber);
        newBusiness.setConfirmPassword(confirmPassword);
        newBusiness.setOrganizationName(organizationName);
        newBusiness.setOrganizationEmail(organizationEmail);
        newBusiness.setFoundUsBy(foundUsBy);
        businessRepository.save(newBusiness);

        organization.setOrganizationName(organizationName);
        organization.setOrganizationDomain(organizationEmail);
        organization.setOrganizationWebsite(null);
        organizationRepository.save(organization);

        admin.setEmail(email);
        admin.setPassword(password);
        admin.setFirstname(firstName);
        admin.setLastname(lastName);
        admin.setPhoneNumber(phoneNumber);
        admin.setRole(Roles.ADMIN);
        admin.setOrganization(organization);
        admin.setOrganizationName(organization.getOrganizationName());
        adminRepository.save(admin);
    }
}
