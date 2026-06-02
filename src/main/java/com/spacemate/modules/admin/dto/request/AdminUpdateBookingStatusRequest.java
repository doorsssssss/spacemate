package com.spacemate.modules.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUpdateBookingStatusRequest {

    @NotNull
    @Min(2)
    @Max(3)
    private Integer status;

    @Size(max = 255)
    private String reason;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}



