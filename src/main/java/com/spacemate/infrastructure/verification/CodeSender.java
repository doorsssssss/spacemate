package com.spacemate.infrastructure.verification;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 楠岃瘉鐮佸彂閫佸櫒鎺ュ彛銆?
 * <p>
 * 鎶借薄鐪熷疄鍙戦€佽涓猴紙鐭俊/閭欢/绔欏唴锛夛紝鏀寔涓嶅悓鍦烘櫙涓庤处鍙锋爣璇嗐€?
 * 榛樿瀹炵幇鍙负鏃ュ織杈撳嚭锛岀敓浜х幆澧冨彲鏇挎崲涓虹涓夋柟鏈嶅姟闆嗘垚銆?
 */
public interface CodeSender {
    /**
     * 鍙戦€侀獙璇佺爜鍒版寚瀹氭爣璇嗐€?
     *
     * @param scene         楠岃瘉鐮佸満鏅紙REGISTER/LOGIN/RESET_PASSWORD锛夈€?
     * @param identifier    鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code          楠岃瘉鐮佸唴瀹广€?
     * @param expireMinutes 楠岃瘉鐮佹湁鏁堟湡锛堝垎閽燂級銆?
     */
    void sendCode(VerificationScene scene, String identifier, String code, int expireMinutes);
}

