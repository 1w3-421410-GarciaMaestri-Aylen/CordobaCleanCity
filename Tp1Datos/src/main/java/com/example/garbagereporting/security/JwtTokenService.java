package com.example.garbagereporting.security;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.model.UserAccount;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final ApplicationProperties properties;

    public JwtTokenResult generate(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAuth().getJwtExpirationMinutes() * 60L);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ecoruta-api")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return new JwtTokenResult(token, expiresAt);
    }
}
