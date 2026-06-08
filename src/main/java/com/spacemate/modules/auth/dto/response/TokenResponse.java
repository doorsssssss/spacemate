package com.spacemate.modules.auth.dto.response;

import java.time.Instant;

/**
 * 登录令牌响应。
 */
public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
