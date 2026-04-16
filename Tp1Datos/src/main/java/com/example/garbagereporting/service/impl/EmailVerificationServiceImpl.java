package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.model.EmailVerificationToken;
import com.example.garbagereporting.model.UserAccount;
import com.example.garbagereporting.repository.EmailVerificationTokenRepository;
import com.example.garbagereporting.service.EmailSenderService;
import com.example.garbagereporting.service.EmailVerificationService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailSenderService emailSenderService;
    private final ApplicationProperties properties;

    @Override
    public void issueVerificationToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAuth().getVerificationTokenExpirationHours() * 3600L);
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();

        emailVerificationTokenRepository.deleteByUserId(user.getId());
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(now)
                .build());

        emailSenderService.sendVerificationEmail(user, buildVerificationLink(token));
    }

    private String buildVerificationLink(String token) {
        String baseUrl = properties.getMail().getVerificationBaseUrl();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return baseUrl + delimiter + "token=" + encodedToken;
    }
}
