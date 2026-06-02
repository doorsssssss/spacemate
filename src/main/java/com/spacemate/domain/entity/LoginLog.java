package com.spacemate.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
@Data
@TableName("login_logs")
public class LoginLog {
    private Long id;
    private Long userId;
    private String identifier;
    private String channel;
    private String ip;
    private String userAgent;
    private String status;
    private Instant createdAt;
}

