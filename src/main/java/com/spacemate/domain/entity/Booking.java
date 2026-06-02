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
@TableName("booking")
public class Booking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bookingNo;

    private Long userId;

    private Long spaceId;

    private Long seatId;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer status;

    private String confirmCode;

    private String cancelReason;

    @TableLogic
    private Integer deleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


