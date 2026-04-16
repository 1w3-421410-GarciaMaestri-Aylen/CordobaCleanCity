package com.example.garbagereporting.service;

import com.example.garbagereporting.dto.AuthLoginRequestDto;
import com.example.garbagereporting.dto.AuthLoginResponseDto;
import com.example.garbagereporting.dto.AuthRegisterRequestDto;
import com.example.garbagereporting.dto.AuthRegisterResponseDto;
import com.example.garbagereporting.dto.AuthUserResponseDto;
import com.example.garbagereporting.dto.EmailVerificationResponseDto;
import java.util.List;

public interface AuthService {

    AuthRegisterResponseDto register(AuthRegisterRequestDto request);

    AuthLoginResponseDto login(AuthLoginRequestDto request);

    EmailVerificationResponseDto verifyEmail(String token);

    EmailVerificationResponseDto resendVerification(String email);

    AuthUserResponseDto getCurrentUser(String email);

    List<AuthUserResponseDto> getAllUsers();
}
