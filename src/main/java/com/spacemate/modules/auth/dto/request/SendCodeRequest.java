package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import com.spacemate.domain.enums.VerificationScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送验证码请求。
 *
 * scene 表示验证码用途：注册、登录或重置密码。
 */
public record SendCodeRequest(
    @NotNull(message = "验证码场景不能为空") VerificationScene scene,
    @NotNull(message = "账号类型不能为空") IdentifierType identifierType,
    @NotBlank(message = "账号不能为空") String identifier
) {
}
