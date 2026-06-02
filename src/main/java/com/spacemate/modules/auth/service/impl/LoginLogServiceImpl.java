package com.spacemate.modules.auth.service.impl;

import com.spacemate.domain.entity.LoginLog;
import com.spacemate.infrastructure.persistence.mapper.LoginLogMapper;
import com.spacemate.modules.auth.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {
    private final LoginLogMapper loginLogMapper;
    /**
     * 璁板綍涓€娆＄櫥褰?娉ㄥ唽浜嬩欢銆?
     *
     * @param userId    鐢ㄦ埛 ID銆?
     * @param identifier 鐧诲綍/娉ㄥ唽浣跨敤鐨勬爣璇嗭紙鎵嬫満鍙锋垨閭锛夈€?
     * @param channel   娓犻亾锛歅ASSWORD/CODE/REGISTER銆?
     * @param ip        瀹㈡埛绔?IP銆?
     * @param userAgent 瀹㈡埛绔?UA銆?
     * @param status    缁撴灉锛歋UCCESS/FAILED銆?
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


