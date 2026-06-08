package com.spacemate.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 */
public record TokenRefreshRequest(@NotBlank(message = "刷新令牌不能为空") String refreshToken) {
}
