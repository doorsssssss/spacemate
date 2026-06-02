package com.spacemate.modules.auth.service.impl;

import com.spacemate.config.AuthProperties;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.TokenPair;
import com.spacemate.modules.auth.service.JwtService;

import org.springframework.security.oauth2.jwt.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class JwtServiceImpl implements JwtService {
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String CLAIM_USER_ID = "uid";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    /**
     * 涓烘寚瀹氱敤鎴风鍙戜竴瀵?Access/Refresh Token銆?
     * @param user
     * @return
     */
    @Override
    public TokenPair issueTokenPair(AppUser user) {
        String refreshTokenId = UUID.randomUUID().toString();
        Instant issuedAt=Instant.now(clock);
        Instant accessExpiresAt =Instant.now().plus(properties.getJwt().getAccessTokenTtl());
        Instant refreshExpiresAt = issuedAt.plus(properties.getJwt().getRefreshTokenTtl());


        String accessToken = encodeToken(user, issuedAt, accessExpiresAt, "access", UUID.randomUUID().toString());
        String refreshToken = encodeRefreshToken(user, issuedAt, refreshExpiresAt, refreshTokenId);

        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, refreshTokenId);

    }

    /**
     * 瑙ｇ爜 JWT 瀛楃涓蹭负 {@link Jwt}銆?
     * @param token JWT 瀛楃涓层€?
     * @return 瑙ｆ瀽鍚庣殑 JWT 瀵硅薄銆?
     */
    @Override
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * 缂栫爜璁块棶浠ょ墝銆?
     *
     * @param user      鐢ㄦ埛瀹炰綋锛屼綔涓?subject 涓庤嚜瀹氫箟澹版槑鏉ユ簮銆?
     * @param issuedAt  绛惧彂鏃堕棿銆?
     * @param expiresAt 杩囨湡鏃堕棿銆?
     * @param tokenType 浠ょ墝绫诲瀷锛?access"锛夈€?
     * @param tokenId   浠ょ墝 ID锛坖ti锛夈€?
     * @return 缂栫爜鍚庣殑 JWT 瀛楃涓层€?
     */
    @Override
    public String encodeToken(AppUser user, Instant issuedAt, Instant expiresAt, String tokenType, String tokenId) {
        JwtClaimsSet claims=JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim(CLAIM_USER_ID, user.getId())
                .claim("nickname", user.getNickname())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 缂栫爜鍒锋柊浠ょ墝銆?
     *
     * @param user      鐢ㄦ埛瀹炰綋銆?
     * @param issuedAt  绛惧彂鏃堕棿銆?
     * @param expiresAt 杩囨湡鏃堕棿銆?
     * @param tokenId   鍒锋柊浠ょ墝 ID锛坖ti锛夈€?
     * @return 缂栫爜鍚庣殑鍒锋柊浠ょ墝瀛楃涓层€?
     */
    @Override
    public String encodeRefreshToken(AppUser user, Instant issuedAt, Instant expiresAt, String tokenId) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .id(tokenId)
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .claim(CLAIM_USER_ID, user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 浠?JWT 涓彁鍙栫敤鎴?ID銆?
     *
     * @param jwt 宸茶В鏋愮殑 JWT銆?
     * @return 鐢ㄦ埛 ID锛坙ong锛夈€?
     * @throws IllegalArgumentException 褰撳０鏄庣被鍨嬩笉鍚堟硶鏃舵姏鍑恒€?
     */
    @Override
    public long extractUserId(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_USER_ID);

        if(claim instanceof Number number){
            return number.longValue();
        }

        if (claim instanceof String text) {
            return Long.parseLong(text);
        }

        throw new IllegalArgumentException("Invalid user id in token");
    }

    /**
     * 鎻愬彇浠ょ墝绫诲瀷澹版槑銆?
     *
     * @param jwt 宸茶В鏋愮殑 JWT銆?
     * @return 浠ょ墝绫诲瀷瀛楃涓诧紙渚嬪锛?access" 鎴?"refresh"锛夈€?
     */
    @Override
    public String extractTokenType(Jwt jwt) {
        Object claim = jwt.getClaims().get(CLAIM_TOKEN_TYPE);
        return claim != null ? claim.toString() : "";
    }

    /**
     * 鎻愬彇浠ょ墝 ID锛坖ti锛夈€?
     *
     * @param jwt 宸茶В鏋愮殑 JWT銆?
     * @return 浠ょ墝 ID銆?
     */
    @Override
    public String extractTokenId(Jwt jwt) {
        return jwt.getId();
    }
}


