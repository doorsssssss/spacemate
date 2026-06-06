package com.spacemate.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("space_comment")
public class SpaceComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;

    private Long userId;

    /**
     * 父评论 ID。为空或 0 表示一级评论；有值表示对某条评论的回复。
     */
    private Long parentId;

    /**
     * 根评论 ID。一级评论的 rootId 为 0；回复使用它来快速归类到同一棵评论树下。
     */
    private Long rootId;

    private String content;

    private Long likeCount;

    private Long replyCount;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}