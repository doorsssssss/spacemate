package com.spacemate.domain.entity;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 发送验证码结果。
 *
 * @param identifier 验证码接收账号。
 * @param scene 验证码场景。
 * @param expireSeconds 过期秒数。
 */
public record SendCodeResult(String identifier,
                             VerificationScene scene,
                             int expireSeconds
) {
}
