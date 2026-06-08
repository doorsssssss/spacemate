package com.spacemate.infrastructure.verification;

import com.spacemate.domain.enums.VerificationScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地开发用验证码发送器。
 *
 * 不真正发送短信或邮件，只把验证码打印到日志里，方便本地调试。
 */
@Slf4j
@Component
public class LoggingCodeSender implements CodeSender {

    @Override
    public void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes) {
        log.info("Send verification code scene={} identifier={} code={} expireMinutes={}", scene, identifier, code, expireMinutes);
    }
}
