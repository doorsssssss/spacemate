package com.spacemate.infrastructure.verification;

import com.spacemate.domain.enums.VerificationScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 寮€鍙?娴嬭瘯鐢ㄩ獙璇佺爜鍙戦€佸櫒銆?
 * <p>
 * 涓嶅疄闄呭彂閫侊紝浠呰褰曟棩蹇楋紝渚夸簬鏈湴寮€鍙戜笌闆嗘垚娴嬭瘯銆?
 */
@Slf4j
@Component
public class LoggingCodeSender implements CodeSender {

    /**
     * 璁板綍楠岃瘉鐮佸彂閫佷俊鎭埌鏃ュ織锛堜笉瀹為檯鍙戦€侊級銆?
     *
     * @param scene         楠岃瘉鐮佸満鏅€?
     * @param identifier    鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code          楠岃瘉鐮佸唴瀹广€?
     * @param expireMinutes 鏈夋晥鏈燂紙鍒嗛挓锛夈€?
     */
    @Override
    public void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes) {
        log.info("Send verification code scene={} identifier={} code={} expireMinutes={}", scene, identifier, code, expireMinutes);
    }
}

