package com.spacemate.infrastructure.verification;

import com.spacemate.domain.entity.VerificationCheckResult;
import java.time.Duration;

/**
 * 验证码存储接口。
 *
 * 用于保存、校验和删除验证码。当前实现使用 Redis。
 */
public interface VerificationCodeStore {
    /**
     * 保存验证码。
     */
    void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts);

    /**
     * 校验验证码。
     */
    VerificationCheckResult verify(String scene, String identifier, String code);

    /**
     * 删除验证码。
     */
    void invalidate(String scene, String identifier);
}
