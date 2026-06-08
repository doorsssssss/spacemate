package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordResetRequest(
    @NotNull(message = "账号类型不能为空") IdentifierType identifierType,
    @NotBlank(message = "账号不能为空") String identifier,
    @NotBlank(message = "验证码不能为空") String code,
    @NotBlank(message = "新密码不能为空") String newPassword
) {
}
