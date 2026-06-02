package com.spacemate.domain.entity;

import java.util.regex.Pattern;

public class IdentifierValidator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private IdentifierValidator() {
    }

    /**
     * 鏍￠獙鎵嬫満鍙锋牸寮忥紙涓浗澶ч檰 11 浣嶏紝浠?1 寮€澶达級銆?
     *
     * @param phone 鎵嬫満鍙峰瓧绗︿覆銆?
     * @return 鏄惁鍖归厤鎵嬫満鍙锋鍒欍€?
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 鏍￠獙閭鏍煎紡锛堝ぇ灏忓啓涓嶆晱鎰燂級銆?
     *
     * @param email 閭瀛楃涓层€?
     * @return 鏄惁鍖归厤閭姝ｅ垯銆?
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
}
}

