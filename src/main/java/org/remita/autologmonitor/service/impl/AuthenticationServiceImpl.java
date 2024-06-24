package org.remita.autologmonitor.service.impl;

import lombok.AllArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import org.remita.autologmonitor.repository.AdminRepository;
import org.remita.autologmonitor.repository.TokenRepository;
import org.remita.autologmonitor.repository.UserRepository;
import org.remita.autologmonitor.service.AuthenticationService;
import org.remita.autologmonitor.service.JWTService;
import org.springframework.security.authentication.AuthenticationManager;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final AdminRepository adminRepository;


}
