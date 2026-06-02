package com.spacemate.modules.auth.service;

import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.TokenPair;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

public interface JwtService {

    TokenPair issueTokenPair(AppUser user);

    Jwt decode(String token);

    String encodeToken(AppUser user, Instant issuedAt, Instant expiresAt, String tokenType, String tokenId);

    String encodeRefreshToken(AppUser user, Instant issuedAt, Instant expiresAt, String tokenId);

    long extractUserId(Jwt jwt);

    String extractTokenType(Jwt jwt);

    String extractTokenId(Jwt jwt);
}

