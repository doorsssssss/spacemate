package com.spacemate.modules.auth.dto.response;

import java.time.LocalDate;

/**
 * 璁よ瘉鐢ㄦ埛鍝嶅簲銆?
 * <p>
 * 闈㈠悜瀹㈡埛绔睍绀虹殑鍩虹鐢ㄦ埛淇℃伅锛屼緵鈥滄垜鏄皝鈥濅笌棣栭〉鏄剧ず浣跨敤銆?
 */
public record AuthUserResponse(
        Long id,
        String nickname,
        String avatar,
        String phone,
        String bio,
        String tagJson
) {
}

