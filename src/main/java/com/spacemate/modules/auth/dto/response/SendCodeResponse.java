package com.spacemate.modules.auth.dto.response;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 鍙戦€侀獙璇佺爜鍝嶅簲銆?
 * <p>
 * 杩斿洖瑙勮寖鍖栧悗鐨勮处鍙枫€佸満鏅紝浠ュ強楠岃瘉鐮佹湁鏁堟湡锛堢锛夈€?
 */
public record SendCodeResponse(
        String identifier,
        VerificationScene scene,
        int expireSeconds
) {
}

