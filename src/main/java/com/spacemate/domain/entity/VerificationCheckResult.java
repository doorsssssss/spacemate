package com.spacemate.domain.entity;

import com.spacemate.domain.enums.VerificationCodeStatus;

/**
 * 验证码校验结果。
 *
 * @param status 校验状态。
 * @param attempts 当前已尝试次数。
 * @param maxAttempts 最大可尝试次数。
 */
public record VerificationCheckResult(
        VerificationCodeStatus status,
        int attempts,
        int maxAttempts
) {
    public boolean isSuccess() {
        return status == VerificationCodeStatus.SUCCESS;
    }
}
