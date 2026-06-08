package com.spacemate.modules.auth.dto.response;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 发送验证码响应。
 */
public record SendCodeResponse(
        String identifier,
        VerificationScene scene,
        int expireSeconds
) {
}
