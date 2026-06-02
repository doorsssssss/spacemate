package com.spacemate.modules.client.dto.request;

import jakarta.validation.constraints.Size;

public class ClientCancelBookingRequest {

    @Size(max = 255)
    private String cancelReason;

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}



