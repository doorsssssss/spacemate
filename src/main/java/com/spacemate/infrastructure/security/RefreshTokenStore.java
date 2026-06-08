package com.spacemate.infrastructure.security;

import java.time.Duration;

/**
 * Refresh Token 存储接口。
 *
 * 用于保存、校验和撤销刷新令牌。
 */
public interface RefreshTokenStore {
    /**
     * 保存刷新令牌。
     */
    void storeToken(long userId, String tokenId, Duration ttl);

    /**
     * 判断刷新令牌是否有效。
     */
    boolean isTokenValid(long userId, String tokenId);

    /**
     * 撤销单个刷新令牌。
     */
    void revokeToken(long userId, String tokenId);

    /**
     * 撤销某个用户的全部刷新令牌。
     */
    void revokeAll(long userId);
}
