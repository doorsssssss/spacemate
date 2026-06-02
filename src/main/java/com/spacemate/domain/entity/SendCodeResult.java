package com.spacemate.domain.entity;

import com.spacemate.domain.enums.VerificationScene;

/**
 * 鍙戦€侀獙璇佺爜缁撴灉銆?
 * <p>
 * 杩斿洖瑙勮寖鍖栬处鍙枫€佸彂閫佸満鏅笌楠岃瘉鐮佹湁鏁堟湡锛堢锛夈€?
 */
public record SendCodeResult(String identifier,
                             VerificationScene scene,
                             int expireSeconds
) {
}

