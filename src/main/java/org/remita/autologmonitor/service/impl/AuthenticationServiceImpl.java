package org.remita.autologmonitor.service.impl;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
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
import org.remita.autologmonitor.util.PasswordStrengthUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final BusinessRepository businessRepository;
    private final AuthenticationManager authenticationManager;
    private final OrganizationRepository organizationRepository;
    private final TokenRepository tokenRepository;
    private final OTPRepository otpRepository;
    private final OtpEmailUtil otpEmailUtil;
    private final OtpUtil otpUtil;
    private final PasswordStrengthUtil passwordStrengthUtil;

    @Override
    public DefaultResponseDto login(LoginRequestDto req) {
        return loginImpl(req);
    }

    @Override
    public DefaultResponseDto signup(SignupRequestDto req) {
        return signupImpl(req);
    }

    private DefaultResponseDto signupImpl(SignupRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        Organization organization = new Organization();
        Admin admin = new Admin();
        Business newBusiness = new Business();
        OTP newOTP = new OTP();

        try {
            boolean userAlreadyExist = adminRepository.findByEmail(req.getEmail()).isPresent();
            log.info("Account exist is: {}", userAlreadyExist);
            validatePasswordAndEmail(userAlreadyExist, req.getPassword());

            DefaultResponseDto validationResponse = validateSignupRequest(req);
            if (validationResponse != null) return validationResponse;

            assignRequestToEntity(req, organization, admin, newBusiness);
            saveEntities(newBusiness, organization, admin);

            String otp = otpUtil.generateOtp();
            saveAndSendOtp(admin, otp, req.getEmail());

            res.setStatus(HttpStatus.SC_CREATED);
            res.setMessage("Onboarding Process Complete, Verify account with OTP sent to mail");
            res.setData(String.format("Business with Id: %s has been created", organization.getOrgId()));

        } catch (Exception e) {
            log.error("Error during signup: {}", e.getMessage());
            res.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            res.setMessage("An error occurred during signup");
            res.setData(e.getMessage());
        }
        return res;
    }

    private DefaultResponseDto loginImpl(LoginRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

            Optional<Admin> optionalAdmin = adminRepository.findByEmail(req.getEmail());
            if (optionalAdmin.isPresent() && optionalAdmin.get().isEnabled()) {
                var admin = optionalAdmin.get();

                validateLoginRequest(req, res);

                var jwtToken = jwtService.generateToken(admin);
                jwtService.generateRefreshToken(new HashMap<>(), admin);

                saveToken(admin, jwtToken);
                revokeAllTokens(admin);

                res.setStatus(HttpStatus.SC_OK);
                res.setMessage("Login Successful");
                res.setData(String.format("Token: %s", jwtToken));
            } else {
                res.setStatus(HttpStatus.SC_UNAUTHORIZED);
                res.setMessage("Invalid credentials or account not enabled");
            }
        } catch (AuthenticationException e) {
            log.error("Authentication error: {}", e.getMessage());
            res.setStatus(HttpStatus.SC_UNAUTHORIZED);
            res.setMessage("Authentication failed");
            res.setData(e.getMessage());
        }
        return res;
    }

    @Override
    public DefaultResponseDto verifyAccount(String email, String otp) {
        log.info("Otp is {} and Email is {}", otp, email);
        DefaultResponseDto res = new DefaultResponseDto();
        try {
            Admin user = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with this email: " + email));
            OTP otpCode = otpRepository.findByUserEntity(user.getId()).orElseThrow(
                    () -> new IllegalArgumentException("OTP not found with this user email: " + email)
            );
            String code = otpCode.getOtpCode();
            boolean expired = new Date().getTime() > otpCode.getExpirationTime().getTime();
            log.info("OTP expiration status: {}", expired);

            if (code.equals(otp) && !expired) {
                enableUserAccount(user, otpCode, res);
            } else {
                handleInvalidOtp(res, expired, code, otp);
            }
        } catch (Exception e) {
            log.error("Error during OTP verification: {}", e.getMessage());
            res.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            res.setMessage("An error occurred during OTP verification");
            res.setData(e.getMessage());
        }
        return res;
    }

    private void validatePasswordAndEmail(boolean accountExist, String password) throws IllegalArgumentException {
        log.info("Checking if account exists");
        if (accountExist) {
            throw new IllegalArgumentException("User already exists, email taken");
        }

        log.info("Checking password strength");
        if (!passwordStrengthUtil.verifyPasswordStrength(password)) {
            throw new IllegalArgumentException("Password should contain at least 8 characters, numbers, and a symbol");
        }
    }

    private DefaultResponseDto validateSignupRequest(SignupRequestDto req) {
        DefaultResponseDto res = new DefaultResponseDto();
        List<String> emptyProperties = checkForEmptyFields(req);
        if (!emptyProperties.isEmpty()) {
            Map<String, String> emptyFields = new HashMap<>();
            for (String property : emptyProperties) {
                emptyFields.put(property, "This field cannot be empty");
            }
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("Invalid Request Made");
            res.setData(emptyFields);
            return res;
        }

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("Invalid Request Made");
            res.setData("Passwords do not match");
            return res;
        }
        return null;
    }

    private void validateLoginRequest(LoginRequestDto req, DefaultResponseDto res) {
        List<String> emptyProperties = checkForEmptyFields(req);
        if (!emptyProperties.isEmpty()) {
            Map<String, String> emptyFields = new HashMap<>();
            for (String property : emptyProperties) {
                emptyFields.put(property, "This field cannot be empty");
            }
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("Invalid Request Made");
            res.setData(emptyFields);
        }
    }

    private List<String> checkForEmptyFields(Object entity) {
        List<String> emptyProperties = new ArrayList<>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.get(entity) == null) {
                    emptyProperties.add(field.getName());
                }
            } catch (IllegalAccessException e) {
                log.error("Field access error: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return emptyProperties;
    }

    private void assignRequestToEntity(SignupRequestDto req, Organization organization, Admin admin, Business newBusiness) {
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

        organization.setOrganizationName(organizationName);
        organization.setOrganizationDomain(organizationEmail);
        organization.setOrganizationWebsite(null);
        organization.setOrgId(generateOrganizationID(organizationName));

        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFirstname(firstName);
        admin.setLastname(lastName);
        admin.setPhoneNumber(phoneNumber);
        admin.setRole(Roles.ADMIN);
        admin.setOrganization(organization);
        admin.setOrganizationName(organization.getOrganizationName());
    }

    private String generateOrganizationID(String orgName) {
        if (orgName.length() < 2) {
            throw new IllegalArgumentException("Organization name must be at least 2 characters long.");
        }
        String prefix = orgName.substring(0, 2).toUpperCase();
        Random random = new Random();
        int randomNumber = random.nextInt(900) + 100;
        return prefix + randomNumber;
    }

    private void saveToken(BaseUserEntity userEntity, String newToken) {
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

    private void revokeAllTokens(BaseUserEntity userEntity) {
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

    private void saveEntities(Business newBusiness, Organization organization, Admin admin) {
        businessRepository.save(newBusiness);
        organizationRepository.save(organization);
        adminRepository.save(admin);
    }

    private void saveAndSendOtp(Admin admin, String otp, String email) throws MessagingException {
        OTP newOTP = new OTP();
        newOTP.setOtpCode(otp);
        newOTP.setAdmin(admin);
        newOTP.setCreatedAt(Date.from(Instant.now()));
        newOTP.setExpirationTime(Date.from(Instant.now().plusSeconds(900)));
        newOTP.setUpdatedAt(null);
        newOTP.setRevoked(false);
        otpRepository.save(newOTP);
        otpEmailUtil.sendOtpEmail(email, otp);
    }

    private void enableUserAccount(Admin user, OTP otpCode, DefaultResponseDto res) {
        user.setEnabled(true);
        adminRepository.save(user);
        otpCode.setRevoked(true);
        otpRepository.save(otpCode);
        res.setStatus(HttpStatus.SC_OK);
        res.setMessage("OTP verified");
        res.setData(otpCode);
    }

    private void handleInvalidOtp(DefaultResponseDto res, boolean expired, String code, String otp) {
        if (expired && code.equals(otp)) {
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("OTP has not been verified, OTP has expired");
        } else if (!code.equals(otp)) {
            res.setStatus(HttpStatus.SC_BAD_REQUEST);
            res.setMessage("OTP has not been verified, Code is not correct");
        }
    }
}