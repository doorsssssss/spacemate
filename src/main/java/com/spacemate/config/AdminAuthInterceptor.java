package com.spacemate.config;

import com.spacemate.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminSecurityProperties adminSecurityProperties;

    public AdminAuthInterceptor(AdminSecurityProperties adminSecurityProperties) {
        this.adminSecurityProperties = adminSecurityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("X-Admin-Token");
        if (!StringUtils.hasText(token) || !token.equals(adminSecurityProperties.getAdminToken())) {
            throw new BusinessException(401, "管理端 Token 无效");
        }
        return true;
    }
}
