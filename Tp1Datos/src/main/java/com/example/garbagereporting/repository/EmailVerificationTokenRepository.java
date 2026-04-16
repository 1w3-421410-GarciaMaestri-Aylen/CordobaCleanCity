package com.example.garbagereporting.repository;

import com.example.garbagereporting.model.EmailVerificationToken;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailVerificationTokenRepository extends MongoRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUserId(String userId);
}
