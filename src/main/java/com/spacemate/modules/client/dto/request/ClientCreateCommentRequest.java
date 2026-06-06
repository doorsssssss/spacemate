package com.spacemate.modules.client.dto.request;

import jakarta.validation.constraints.Size;

public class ClientCreateCommentRequest {

    private Long parentId;

    @Size(min = 1, max = 500)
    private String content;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}