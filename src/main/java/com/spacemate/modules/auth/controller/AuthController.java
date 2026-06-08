package com.spacemate.modules.auth.controller;

import com.spacemate.domain.entity.ClientInfo;
import com.spacemate.modules.auth.dto.request.LoginRequest;
import com.spacemate.modules.auth.dto.request.LogoutRequest;
import com.spacemate.modules.auth.dto.request.PasswordResetRequest;
import com.spacemate.modules.auth.dto.request.RegisterRequest;
import com.spacemate.modules.auth.dto.request.SendCodeRequest;
import com.spacemate.modules.auth.dto.request.TokenRefreshRequest;
import com.spacemate.modules.auth.dto.response.AuthResponse;
import com.spacemate.modules.auth.dto.response.AuthUserResponse;
import com.spacemate.modules.auth.dto.response.SendCodeResponse;
import com.spacemate.modules.auth.dto.response.TokenResponse;
import com.spacemate.modules.auth.service.AuthService;
import com.spacemate.modules.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 发送验证码。
     */
    @PostMapping("/send-code")
    public SendCodeResponse sendCode(@Valid @RequestBody SendCodeRequest request) {
        return authService.sendCode(request);
    }

    /**
     * 用户注册，注册成功后直接返回登录态令牌。
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, resolveClient(httpRequest));
    }

    /**
     * 用户登录，支持密码登录和验证码登录。
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClient(httpRequest));
    }

    /**
     * 使用 Refresh Token 刷新令牌。
     */
    @PostMapping("/token/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    /**
     * 用户登出，撤销当前 Refresh Token。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * 重置密码。
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 查询当前登录用户信息。
     */
    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return authService.me(userId);
    }

    private ClientInfo resolveClient(HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        return new ClientInfo(ip, ua);
    }

    private String extractClientIp(HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        String realIp = httpRequest.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }
}
