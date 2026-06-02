package com.spacemate.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
@TableName("space")
public class Space {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private LocalTime openStartTime;

    private LocalTime openEndTime;

    private String wifiSsid;

    private String wifiPassword;

    private String rules;

    private BigDecimal priceHourly;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime deletedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}


