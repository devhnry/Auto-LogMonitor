package org.remita.autologmonitor.service;


import org.remita.autologmonitor.dto.DefaultResponseDto;
import org.remita.autologmonitor.dto.LoginRequestDto;
import org.remita.autologmonitor.dto.SignupRequestDto;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {
    DefaultResponseDto signup(SignupRequestDto req);
    DefaultResponseDto login(LoginRequestDto req);
}
