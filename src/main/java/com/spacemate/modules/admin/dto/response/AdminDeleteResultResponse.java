package com.spacemate.modules.admin.dto.response;

public class AdminDeleteResultResponse {

    private Long id;
    private Boolean deleted;

    public AdminDeleteResultResponse() {
    }

    public AdminDeleteResultResponse(Long id, Boolean deleted) {
        this.id = id;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}


