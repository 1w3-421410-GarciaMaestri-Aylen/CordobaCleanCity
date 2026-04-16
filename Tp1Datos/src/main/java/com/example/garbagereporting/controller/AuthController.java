package com.example.garbagereporting.controller;

import com.example.garbagereporting.dto.AuthLoginRequestDto;
import com.example.garbagereporting.dto.AuthLoginResponseDto;
import com.example.garbagereporting.dto.AuthRegisterRequestDto;
import com.example.garbagereporting.dto.AuthRegisterResponseDto;
import com.example.garbagereporting.dto.AuthUserResponseDto;
import com.example.garbagereporting.dto.EmailVerificationResponseDto;
import com.example.garbagereporting.dto.ResendVerificationRequestDto;
import com.example.garbagereporting.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthRegisterResponseDto> register(@Valid @RequestBody AuthRegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDto> login(@Valid @RequestBody AuthLoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponseDto> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<EmailVerificationResponseDto> resendVerification(
            @Valid @RequestBody ResendVerificationRequestDto request
    ) {
        return ResponseEntity.ok(authService.resendVerification(request.getEmail()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponseDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getCurrentUser(jwt.getSubject()));
    }
}
