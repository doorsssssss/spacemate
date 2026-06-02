package com.spacemate.common.api;

import java.time.OffsetDateTime;
import org.slf4j.MDC;

public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private String traceId;
    private String path;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("ok");
        response.setData(data);
        response.setTraceId(MDC.get("traceId"));
        return response;
    }

    public static <T> ApiResponse<T> error(Integer code, String message, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        response.setTraceId(MDC.get("traceId"));
        response.setPath(path);
        response.setTimestamp(OffsetDateTime.now().toString());
        return response;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}


