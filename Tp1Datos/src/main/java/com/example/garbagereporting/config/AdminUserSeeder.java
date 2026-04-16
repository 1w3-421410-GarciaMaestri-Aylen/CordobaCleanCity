package com.example.garbagereporting.config;

import com.example.garbagereporting.model.UserAccount;
import com.example.garbagereporting.model.UserRole;
import com.example.garbagereporting.repository.UserAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminUserSeeder {

    private final ApplicationProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAdminUser() {
        return args -> {
            var adminConfig = properties.getAuth().getAdmin();
            String normalizedEmail = adminConfig.getEmail().trim().toLowerCase();
            Instant now = Instant.now();

            UserAccount existing = userAccountRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
            if (existing != null) {
                existing.setFirstName(adminConfig.getFirstName().trim());
                existing.setLastName(adminConfig.getLastName().trim());
                existing.setRole(UserRole.ADMIN);
                existing.setEmailVerified(true);
                existing.setPasswordHash(passwordEncoder.encode(adminConfig.getPassword()));
                existing.setUpdatedAt(now);
                userAccountRepository.save(existing);
                log.info("Admin user refreshed with configured credentials for email={}", normalizedEmail);
                return;
            }

            UserAccount admin = UserAccount.builder()
                    .firstName(adminConfig.getFirstName().trim())
                    .lastName(adminConfig.getLastName().trim())
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(adminConfig.getPassword()))
                    .role(UserRole.ADMIN)
                    .emailVerified(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            userAccountRepository.save(admin);
            log.info("Admin user seeded with email={}", normalizedEmail);
        };
    }
}
