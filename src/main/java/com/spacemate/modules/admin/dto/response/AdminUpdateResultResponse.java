package com.spacemate.modules.admin.dto.response;

public class AdminUpdateResultResponse {

    private Long id;
    private Boolean updated;

    public AdminUpdateResultResponse() {
    }

    public AdminUpdateResultResponse(Long id, Boolean updated) {
        this.id = id;
        this.updated = updated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }
}


