package com.spacemate.domain.entity;

import java.time.Instant;

/**
 * 璁块棶浠ょ墝涓庡埛鏂颁护鐗岀殑缁勫悎銆?
 * @param accessToken 璁块棶浠ょ墝锛圝WT 瀛楃涓诧紝Bearer 浣跨敤锛夛紱
 * @param accessTokenExpiresAt
 * @param refreshToken 鍒锋柊浠ょ墝锛圝WT 瀛楃涓诧紝浠呯敤浜庡埛鏂版帴鍙ｏ級锛?
 * @param refreshTokenExpiresAt
 * @param refreshTokenId
 */
public record TokenPair(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String refreshTokenId
) {
}

