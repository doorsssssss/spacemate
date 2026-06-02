package com.spacemate.modules.admin.dto.response;

import java.time.LocalDateTime;

public class AdminSeatResponse {

    private Long id;
    private Long spaceId;
    private String seatNumber;
    private Boolean hasSocket;
    private Boolean isQuiet;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Boolean getHasSocket() {
        return hasSocket;
    }

    public void setHasSocket(Boolean hasSocket) {
        this.hasSocket = hasSocket;
    }

    public Boolean getIsQuiet() {
        return isQuiet;
    }

    public void setIsQuiet(Boolean isQuiet) {
        this.isQuiet = isQuiet;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


