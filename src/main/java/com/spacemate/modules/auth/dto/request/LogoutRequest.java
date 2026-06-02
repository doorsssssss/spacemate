package com.spacemate.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 鐧诲嚭璇锋眰銆?
 * <p>
 * 浼犲叆鍒锋柊浠ょ墝浠ユ挙閿€瀵瑰簲浼氳瘽锛岀‘淇濅护鐗屼笉鍙啀鐢ㄣ€?
 */
public record LogoutRequest(@NotBlank(message = "鍒锋柊浠ょ墝涓嶈兘涓虹┖") String refreshToken) {
}

