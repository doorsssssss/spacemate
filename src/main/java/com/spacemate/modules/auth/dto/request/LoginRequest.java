package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 登录请求。
 *
 * 支持两种登录方式：
 * - 验证码登录：传入 code。
 * - 密码登录：传入 password。
 * identifierType 与 identifier 用于确定登录账号，例如手机号或邮箱。
 */
public record LoginRequest(
    @NotNull(message = "账号类型不能为空") IdentifierType identifierType,
    @NotBlank(message = "账号不能为空") String identifier,
    String code,
    String password
) {
}
