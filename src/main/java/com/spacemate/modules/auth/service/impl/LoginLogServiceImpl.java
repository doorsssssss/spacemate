package com.spacemate.modules.auth.service.impl;

import com.spacemate.domain.entity.LoginLog;
import com.spacemate.infrastructure.persistence.mapper.LoginLogMapper;
import com.spacemate.modules.auth.service.LoginLogService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {
    private final LoginLogMapper loginLogMapper;

    /**
     * 记录登录、注册等认证行为日志。
     *
     * @param userId 用户 ID。
     * @param identifier 登录/注册使用的账号标识。
     * @param channel 登录渠道，例如 PASSWORD、CODE、REGISTER。
     * @param ip 客户端 IP。
     * @param userAgent 客户端 UA。
     * @param status 结果状态，例如 SUCCESS、FAILED。
     */
    @Override
    @Transactional
    public void record(Long userId, String identifier, String channel, String ip, String userAgent, String status) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setIdentifier(identifier);
        loginLog.setChannel(channel);
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setStatus(status);
        loginLog.setCreatedAt(Instant.now());
        loginLogMapper.insert(loginLog);
    }
}
