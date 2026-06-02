package com.spacemate.common.exception;

import com.spacemate.common.error.ErrorCode;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode != null ? errorCode.name() : "Business error");
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = resolveCode(errorCode);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return code;
    }

    private int resolveCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return 500;
        }
        return switch (errorCode) {
            case INVALID_CREDENTIALS, REFRESH_TOKEN_INVALID -> 401;
            case INTERNAL_ERROR -> 500;
            default -> 400;
        };
    }
}


