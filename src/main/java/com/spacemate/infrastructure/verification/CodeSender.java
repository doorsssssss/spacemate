package com.spacemate.infrastructure.verification;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 验证码发送接口。
 *
 * 基础版项目使用日志打印验证码，后续可以替换为短信、邮箱或站内信发送实现。
 */
public interface CodeSender {
    /**
     * 发送验证码。
     *
     * @param scene 验证码场景。
     * @param identifier 接收验证码的账号，如手机号或邮箱。
     * @param code 验证码。
     * @param expireMinutes 过期分钟数。
     */
    void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes);
}
