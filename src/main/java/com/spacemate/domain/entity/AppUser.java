package com.spacemate.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String nickname;

    private String avatar;

    private String bio;

    private String tagsJson;

    private String email;

    private Integer role;

    private Integer status;

    private LocalDateTime lastLoginAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private String passwordHash;



    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


