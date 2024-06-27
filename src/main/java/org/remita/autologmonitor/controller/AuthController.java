package org.remita.autologmonitor.controller;

import lombok.AllArgsConstructor;
import org.remita.autologmonitor.dto.DefaultResponseDto;
import org.remita.autologmonitor.dto.LoginRequestDto;
import org.remita.autologmonitor.dto.SignupRequestDto;
import org.remita.autologmonitor.service.AuthenticationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public DefaultResponseDto signup(@RequestBody SignupRequestDto requestDto){
        return authenticationService.signup(requestDto);
    }

    @PostMapping("/login")
    public DefaultResponseDto login(@RequestBody LoginRequestDto requestDto){
        return authenticationService.login(requestDto);
    }

    @PostMapping("verify-account")
    public DefaultResponseDto verifyAccount(@RequestBody VerifyAccountDto){

    }
}
