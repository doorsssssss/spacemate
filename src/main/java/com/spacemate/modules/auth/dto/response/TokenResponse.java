package com.spacemate.modules.auth.dto.response;

import java.time.Instant;

/**
 * 浠ょ墝鍝嶅簲銆?
 * <p>
 * 杩斿洖璁块棶浠ょ墝涓庡埛鏂颁护鐗屽強鍏惰繃鏈熸椂闂达紝渚涘鎴风鎸佷箙鍖栦笌鍚庣画璋冪敤浣跨敤銆?
 */
public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}

