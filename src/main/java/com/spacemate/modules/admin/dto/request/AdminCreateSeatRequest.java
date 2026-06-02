package com.spacemate.modules.admin.dto.request;


import jakarta.validation.constraints.*;

public class AdminCreateSeatRequest {

    @NotNull
    private Long spaceId;

    @NotBlank
    @Size(max = 20)
    private String seatNumber;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer hasSocket;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer isQuiet;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public String getSeatNumber() {
        return seatNumber;
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


