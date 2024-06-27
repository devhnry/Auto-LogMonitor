package org.remita.autologmonitor.service.impl;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.jvnet.hk2.annotations.Service;
import org.remita.autologmonitor.dto.DefaultResponseDto;
import org.remita.autologmonitor.dto.LoginRequestDto;
import org.remita.autologmonitor.dto.SignupRequestDto;
import org.remita.autologmonitor.entity.*;
import org.remita.autologmonitor.enums.Roles;
import org.remita.autologmonitor.enums.TokenType;
import org.remita.autologmonitor.repository.*;
import org.remita.autologmonitor.service.AuthenticationService;
import org.remita.autologmonitor.service.JWTService;
import org.remita.autologmonitor.util.OtpEmailUtil;
import org.remita.autologmonitor.util.OtpUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final BusinessRepository businessRepository;
    private final AuthenticationManager authenticationManager;
    private final OrganizationRepository organizationRepository;
    private final TokenRepository tokenRepository;
    private final OTPRepository otpRepository;
    private final OtpEmailUtil otpEmailUtil;
    private final OtpUtil otpUtil;

    @Override
    public DefaultResponseDto login(LoginRequestDto req) { return loginImpl(req); }

    @Override
    public DefaultResponseDto signup(SignupRequestDto req){ return  signupImpl(req); }

    private DefaultResponseDto signupImpl(SignupRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        Organization organization = new Organization();
        Admin admin = new Admin();
        Business newBusiness = new Business();
        OTP newOTP = new OTP();

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

        String otp = otpUtil.generateOtp();

        newOTP.setOtpCode(otp);
        newOTP.setAdmin(admin);
        newOTP.setCreatedAt(Date.from(Instant.now()));
        newOTP.setExpirationTime(Date.from(Instant.now().plusSeconds(900)));
        newOTP.setUpdatedAt(null);
        newOTP.setRevoked(false);

        otpRepository.save(newOTP);

        try {
            otpEmailUtil.sendOtpEmail(req.getEmail(), otp);
        } catch (MessagingException e) {
            throw new RuntimeException("Unable to send otp please try again");
        }

        res.setStatus(HttpStatus.SC_CREATED);
        res.setMessage("Onboarding Process Complete, Verify account with OTP sent to mail");
        res.setData(String.format("Business with Id: {} has been created", newBusiness.getId()));
        return res;
    }

    @Override
    public DefaultResponseDto verifyAccount(String email, String otp) {

        DefaultResponseDto res = new DefaultResponseDto();
        Admin user = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email: " + email));
        OTP otpCode = otpRepository.findByUserEntity(user.getId()).orElseThrow(
                () -> new RuntimeException("OTP not found with this user email: " + email)
        );
        String code = otpCode.getOtpCode();
        long differenceInMillis = otpCode.getCreatedAt().getTime() - otpCode.getExpirationTime().getTime();
        long differenceInSeconds = differenceInMillis / 1000;


        if (code.equals(otp) && differenceInSeconds < 900) {
            user.setEnabled(true);
            otpCode.setRevoked(true);

            otpRepository.save(otpCode);
            res.setStatus(HttpStatus.SC_OK);
            res.setMessage("OTP verified");
            res.setData(otpCode);

            return res;
        }

        res.setStatus(HttpStatus.SC_BAD_REQUEST);
        if(!code.equals(otp)){
            res.setMessage("OTP has not been verified, Code is not correct");
        }else if(differenceInSeconds > 900 && code.equals(otp)){
            res.setMessage("OTP has not been verified, Code has expired");
        }
        res.setData(otpCode);

        return res;
    }

    private DefaultResponseDto loginImpl(LoginRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                req.getEmail(), req.getPassword()));

        Optional<Admin> optionalAdmin = adminRepository.findByEmail(req.getEmail());
        if (optionalAdmin.isPresent()) {
            var admin = optionalAdmin.orElseThrow();

            LoginRequestDto request = req;
            List<String> emptyProperties = hasNoNullProperties(req);
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

            var jwtToken = jwtService.generateToken(admin);
            jwtService.generateRefreshToken(new HashMap<>(), admin);

            saveToken(admin, jwtToken);
            revokeAllTokens(admin);

            res.setStatus(HttpStatus.SC_OK);
            res.setMessage("Login Successful");
            res.setData(String.format("Token: {}", jwtToken));
        }
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

    public String generateOrganizationID(String orgName) {
        if (orgName.length() < 2) {
            throw new IllegalArgumentException("Organization name must be at least 2 characters long.");
        }
        String prefix = orgName.substring(0, 2).toUpperCase();
        Random random = new Random();
        int randomNumber = random.nextInt(900) + 100;
        return prefix + randomNumber;
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
        organization.setOrganizationId(generateOrganizationID(organizationName));
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

    public void saveToken(BaseUserEntity userEntity, String newToken) {
        Token token;
        if (userEntity instanceof User) {
            token = Token.builder()
                    .users((User) userEntity)
                    .token(newToken)
                    .tokenType(TokenType.BEARER)
                    .expired(false)
                    .revoked(false)
                    .build();
        } else if (userEntity instanceof Admin) {
            token = Token.builder()
                    .admin((Admin) userEntity)
                    .token(newToken)
                    .tokenType(TokenType.BEARER)
                    .expired(false)
                    .revoked(false)
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported user entity type");
        }
        tokenRepository.save(token);
    }

    public void revokeAllTokens(BaseUserEntity userEntity) {
        var validTokens = tokenRepository.findValidTokenByUserEntity(userEntity.getId());
        if (validTokens.isEmpty()) {
            return;
        }
        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

}
