package com.spacemate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * 认证相关配置。
 *
 * 配置前缀为 {@code auth}，主要包含：
 * - Jwt：令牌签发、过期时间、密钥配置。
 * - Verification：验证码长度、有效期、发送频率限制。
 * - Password：密码加密强度与最小长度。
 */
@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    /** JWT 配置。 */
    private final Jwt jwt = new Jwt();
    /** 验证码配置。 */
    private final Verification verification = new Verification();
    /** 密码策略配置。 */
    private final Password password = new Password();

    @Data
    public static class Jwt {
        /** JWT 签发方。 */
        private String issuer = "SPACE_MATE";
        /** 访问令牌有效期。 */
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        /** 刷新令牌有效期。 */
        private Duration refreshTokenTtl = Duration.ofDays(7);
        /** JWT 密钥 ID。 */
        private String keyId = "SPACE_MATE_KEY";
        /** RSA 私钥 PEM，建议使用 PKCS#8 格式。 */
        private Resource privateKey;
        /** RSA 公钥 PEM，建议使用 X.509 格式。 */
        private Resource publicKey;
    }

    /**
     * 验证码策略配置。
     */
    @Data
    public static class Verification {
        /** 验证码长度。 */
        private int codeLength = 6;
        /** 验证码有效期。 */
        private Duration ttl = Duration.ofMinutes(5);
        /** 单个验证码最多尝试次数。 */
        private int maxAttempts = 5;
        /** 同一场景同一账号发送间隔。 */
        private Duration sendInterval = Duration.ofSeconds(60);
        /** 同一场景同一账号每日发送上限。 */
        private int dailyLimit = 10;
    }

    /** 密码策略配置。 */
    @Data
    public static class Password {
        /** BCrypt 加密强度。 */
        private int bcryptStrength = 12;
        /** 密码最小长度。 */
        private int minLength = 8;
    }
}
