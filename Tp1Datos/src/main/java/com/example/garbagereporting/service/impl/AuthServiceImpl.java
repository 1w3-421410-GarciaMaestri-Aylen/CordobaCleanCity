package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.dto.AuthLoginRequestDto;
import com.example.garbagereporting.dto.AuthLoginResponseDto;
import com.example.garbagereporting.dto.AuthRegisterRequestDto;
import com.example.garbagereporting.dto.AuthRegisterResponseDto;
import com.example.garbagereporting.dto.AuthUserResponseDto;
import com.example.garbagereporting.dto.EmailVerificationResponseDto;
import com.example.garbagereporting.exception.BusinessValidationException;
import com.example.garbagereporting.exception.ForbiddenException;
import com.example.garbagereporting.exception.ResourceNotFoundException;
import com.example.garbagereporting.exception.UnauthorizedException;
import com.example.garbagereporting.model.UserAccount;
import com.example.garbagereporting.model.UserRole;
import com.example.garbagereporting.repository.EmailVerificationTokenRepository;
import com.example.garbagereporting.repository.UserAccountRepository;
import com.example.garbagereporting.security.JwtTokenResult;
import com.example.garbagereporting.security.JwtTokenService;
import com.example.garbagereporting.service.AuthService;
import com.example.garbagereporting.service.EmailVerificationService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @Override
    public AuthRegisterResponseDto register(AuthRegisterRequestDto request) {
        String email = normalizeEmail(request.getEmail());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessValidationException("email is already registered");
        }

        Instant now = Instant.now();
        UserAccount user = UserAccount.builder()
                .firstName(cleanText(request.getFirstName()))
                .lastName(cleanText(request.getLastName()))
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .emailVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserAccount savedUser = userAccountRepository.save(user);
        emailVerificationService.issueVerificationToken(savedUser);

        return AuthRegisterResponseDto.builder()
                .email(savedUser.getEmail())
                .verificationRequired(true)
                .message("Usuario registrado. Revisa tu email para verificar la cuenta.")
                .build();
    }

    @Override
    public AuthLoginResponseDto login(AuthLoginRequestDto request) {
        String email = normalizeEmail(request.getEmail());
        try {
            UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

            if (!user.isEmailVerified()) {
                throw new ForbiddenException("Email not verified");
            }

            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
            } catch (AuthenticationException ex) {
                throw new UnauthorizedException("Invalid credentials");
            }

            JwtTokenResult token = jwtTokenService.generate(user);

            return AuthLoginResponseDto.builder()
                    .tokenType("Bearer")
                    .accessToken(token.tokenValue())
                    .expiresAt(token.expiresAt())
                    .user(toUserResponse(user))
                    .build();
        } catch (RuntimeException ex) {
            log.error("Login failed for email={}", email, ex);
            throw ex;
        }
    }

    @Override
    public EmailVerificationResponseDto verifyEmail(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessValidationException("verification token is required");
        }

        var verificationToken = emailVerificationTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResourceNotFoundException("verification token not found"));

        if (verificationToken.isUsed()) {
            throw new BusinessValidationException("verification token was already used");
        }
        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessValidationException("verification token expired");
        }

        UserAccount user = userAccountRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        user.setEmailVerified(true);
        user.setUpdatedAt(Instant.now());
        userAccountRepository.save(user);

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(verificationToken);

        return EmailVerificationResponseDto.builder()
                .verified(true)
                .message("Email verificado correctamente. Ya puedes iniciar sesion.")
                .build();
    }

    @Override
    public EmailVerificationResponseDto resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        if (user.isEmailVerified()) {
            return EmailVerificationResponseDto.builder()
                    .verified(true)
                    .message("El email ya estaba verificado.")
                    .build();
        }

        emailVerificationService.issueVerificationToken(user);

        return EmailVerificationResponseDto.builder()
                .verified(false)
                .message("Se envio un nuevo email de verificacion.")
                .build();
    }

    @Override
    public AuthUserResponseDto getCurrentUser(String email) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        return toUserResponse(user);
    }

    @Override
    public List<AuthUserResponseDto> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        UserAccount::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(this::toUserResponse)
                .toList();
    }

    private AuthUserResponseDto toUserResponse(UserAccount user) {
        return AuthUserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }
}
