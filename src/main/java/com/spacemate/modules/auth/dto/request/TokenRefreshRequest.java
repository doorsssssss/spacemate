package com.spacemate.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 鍒锋柊浠ょ墝璇锋眰銆?
 * <p>
 * 浼犲叆鏃х殑鍒锋柊浠ょ墝锛屾湇鍔″櫒楠岃瘉鍚庤繑鍥炴柊鐨勮闂?鍒锋柊浠ょ墝瀵广€?
 */
public record TokenRefreshRequest(@NotBlank(message = "鍒锋柊浠ょ墝涓嶈兘涓虹┖") String refreshToken) {
}

