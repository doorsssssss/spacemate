package com.spacemate.modules.client.dto.response;

public class ClientCommentLikeResponse {

    private Long commentId;
    private Boolean liked;
    private Boolean changed;
    private Long likeCount;

    public ClientCommentLikeResponse() {
    }

    public ClientCommentLikeResponse(Long commentId, Boolean liked, Boolean changed, Long likeCount) {
        this.commentId = commentId;
        this.liked = liked;
        this.changed = changed;
        this.likeCount = likeCount;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }
}