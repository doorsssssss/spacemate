package com.spacemate.modules.auth.dto.response;

/**
 * 认证用户响应。
 */
public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String bio,
        String tagJson
) {
}
