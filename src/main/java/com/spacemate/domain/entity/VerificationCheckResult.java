package com.spacemate.domain.entity;

import com.spacemate.domain.enums.VerificationCodeStatus;

/**
 * 楠岃瘉鐮佹牎楠岀粨鏋溿€?
 * <p>
 * 鍖呭惈鐘舵€侊紙鎴愬姛/鏈壘鍒?杩囨湡/閿欒/灏濊瘯杩囧锛夊拰娆℃暟缁熻淇℃伅锛屾彁渚涗究鎹锋垚鍔熷垽鏂€?
 */
public record VerificationCheckResult(
        VerificationCodeStatus status,
        int attempts,
        int maxAttempts
) {
    public boolean isSuccess() {
        return status == VerificationCodeStatus.SUCCESS;
    }
}


