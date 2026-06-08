package com.spacemate.modules.auth.service;

public interface LoginLogService {
    /**
     * 记录认证行为日志。
     *
     * @param userId 用户 ID。
     * @param identifier 登录/注册使用的账号标识。
     * @param channel 渠道，例如 PASSWORD、CODE、REGISTER。
     * @param ip 客户端 IP。
     * @param userAgent 客户端 UA。
     * @param status 结果状态，例如 SUCCESS、FAILED。
     */
    void record(Long userId, String identifier, String channel, String ip, String userAgent, String status);
}
