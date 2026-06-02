package com.spacemate.modules.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminUpdateSeatRequest {

    private Long spaceId;

    @Size(max = 20)
    private String seatNumber;

    @Min(0)
    @Max(1)
    private Integer hasSocket;

    @Min(0)
    @Max(1)
    private Integer isQuiet;

    @Min(0)
    @Max(1)
    private Integer status;

    public String getSeatNumber() {
        return seatNumber;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getHasSocket() {
        return hasSocket;
    }

    public void setHasSocket(Integer hasSocket) {
        this.hasSocket = hasSocket;
    }

    public Integer getIsQuiet() {
        return isQuiet;
    }

    public void setIsQuiet(Integer isQuiet) {
        this.isQuiet = isQuiet;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}


