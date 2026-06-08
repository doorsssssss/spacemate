package com.spacemate.modules.auth.service.impl;

import static com.spacemate.domain.enums.IdentifierType.EMAIL;
import static com.spacemate.domain.enums.IdentifierType.PHONE;

import com.spacemate.common.error.ErrorCode;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.config.AuthProperties;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.ClientInfo;
import com.spacemate.domain.entity.IdentifierValidator;
import com.spacemate.domain.entity.TokenPair;
import com.spacemate.domain.entity.VerificationCheckResult;
import com.spacemate.domain.enums.IdentifierType;
import com.spacemate.domain.enums.VerificationCodeStatus;
import com.spacemate.domain.enums.VerificationScene;
import com.spacemate.infrastructure.security.RefreshTokenStore;
import com.spacemate.modules.auth.dto.request.LoginRequest;
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
import com.spacemate.modules.auth.service.LoginLogService;
import com.spacemate.modules.auth.service.UserService;
import com.spacemate.modules.auth.service.VerificationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties authProperties;
    private final VerificationService verificationService;
    private final UserService userService;

    @Override
    public AuthResponse register(RegisterRequest request, ClientInfo clientInfo) {
        // 注册必须同意服务协议。
        if (!request.agreeTerms()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
        }
        validateIdentifier(request.identifierType(), request.identifier());

        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());

        if (identifierExists(request.identifierType(), request.identifier())) {
            throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
        }
        ensureVerificationSuccess(verificationService.verify(VerificationScene.REGISTER, identifier, request.code()));

        AppUser appUser = new AppUser();
        appUser.setPhone(request.identifierType() == PHONE ? identifier : null);
        appUser.setEmail(request.identifierType() == EMAIL ? identifier : null);
        appUser.setNickname(generateNickname());
        appUser.setBio(null);
        appUser.setAvatar(null);
        appUser.setTagsJson("[]");

        if (StringUtils.hasText(request.password())) {
            validatePassword(request.password());
            appUser.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        userService.createUser(appUser);
        TokenPair tokenPair = jwtService.issueTokenPair(appUser);
        storeRefreshToken(appUser.getId(), tokenPair);
        loginLogService.record(appUser.getId(), identifier, "REGISTER", clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");

        return new AuthResponse(mapUser(appUser), mapToken(tokenPair));
    }

    @Override
    public SendCodeResponse sendCode(SendCodeRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        String normalized = normalizeIdentifier(request.identifierType(), request.identifier());

        boolean exists = identifierExists(request.identifierType(), request.identifier());

        if (request.scene() == VerificationScene.REGISTER && exists) {
            throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
        }
        if ((request.scene() == VerificationScene.LOGIN || request.scene() == VerificationScene.RESET_PASSWORD) && !exists) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        var result = verificationService.sendCode(request.scene(), normalized);
        return new SendCodeResponse(result.identifier(), result.scene(), result.expireSeconds());
    }

    /**
     * 登出：如果传入的是有效 refresh token，则撤销 Redis 中对应的令牌记录。
     */
    public void logout(String refreshToken) {
        decodeRefreshTokenSafely(refreshToken).ifPresent(jwt -> {
            if (Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
                long userId = jwtService.extractUserId(jwt);
                String tokenId = jwtService.extractTokenId(jwt);
                refreshTokenStore.revokeToken(userId, tokenId);
            }
        });
    }

    /**
     * 查询当前登录用户信息。
     */
    public AuthUserResponse me(long userId) {
        AppUser user = findUserById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        return mapUser(user);
    }

    /**
     * 登录：支持密码登录和验证码登录。
     */
    public AuthResponse login(LoginRequest request, ClientInfo clientInfo) {
        validateIdentifier(request.identifierType(), request.identifier());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        Optional<AppUser> userOptional = findUserByIdentifier(request.identifierType(), request.identifier());
        if (userOptional.isEmpty()) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        AppUser user = userOptional.get();
        String channel;

        if (StringUtils.hasText(request.password())) {
            channel = "PASSWORD";
            if (!StringUtils.hasText(user.getPasswordHash()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                loginLogService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "FAILED");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        } else if (StringUtils.hasText(request.code())) {
            channel = "CODE";
            ensureVerificationSuccess(verificationService.verify(VerificationScene.LOGIN, identifier, request.code()));
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供密码或验证码");
        }
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        storeRefreshToken(user.getId(), tokenPair);
        loginLogService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    /**
     * 使用 refresh token 刷新 access token，并轮换 refresh token。
     */
    public TokenResponse refresh(TokenRefreshRequest request) {
        Jwt jwt = decodeRefreshToken(request.refreshToken());

        if (!Objects.equals("refresh", jwtService.extractTokenType(jwt))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        long userId = jwtService.extractUserId(jwt);
        String tokenId = jwtService.extractTokenId(jwt);

        if (!refreshTokenStore.isTokenValid(userId, tokenId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        AppUser user = findUserById(userId).orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        TokenPair tokenPair = jwtService.issueTokenPair(user);
        refreshTokenStore.revokeToken(userId, tokenId);
        storeRefreshToken(userId, tokenPair);

        return mapToken(tokenPair);
    }

    /**
     * 重置密码：先校验账号和新密码，再校验验证码，最后更新密码并撤销该用户所有 refresh token。
     */
    public void resetPassword(PasswordResetRequest request) {
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.newPassword());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        AppUser user = findUserByIdentifier(request.identifierType(), request.identifier())
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        ensureVerificationSuccess(verificationService.verify(VerificationScene.RESET_PASSWORD, identifier, request.code()));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        userService.updatePassword(user);
        refreshTokenStore.revokeAll(user.getId());
    }

    private TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.accessTokenExpiresAt(), tokenPair.refreshToken(), tokenPair.refreshTokenExpiresAt());
    }

    private void storeRefreshToken(Long userId, TokenPair tokenPair) {
        Duration ttl = Duration.between(Instant.now(), tokenPair.refreshTokenExpiresAt());
        if (ttl.isNegative()) {
            ttl = Duration.ZERO;
        }

        refreshTokenStore.storeToken(userId, tokenPair.refreshTokenId(), ttl);
    }

    private AuthUserResponse mapUser(AppUser user) {
        return new AuthUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getBio(),
                user.getTagsJson()
        );
    }

    private String generateNickname() {
        return "SpaceMate" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码不能为空");
        }
        String trimmed = password;

        if (trimmed.length() < authProperties.getPassword().getMinLength()) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码长度不足");
        }
        boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "密码必须同时包含字母和数字");
        }
    }

    private void ensureVerificationSuccess(VerificationCheckResult result) {
        if (result.isSuccess()) {
            return;
        }
        VerificationCodeStatus status = result.status();
        if (status == VerificationCodeStatus.NOT_FOUND || status == VerificationCodeStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
        if (status == VerificationCodeStatus.MISMATCH) {
            throw new BusinessException(ErrorCode.VERIFICATION_MISMATCH);
        }
        if (status == VerificationCodeStatus.TOO_MANY_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码校验失败");
    }

    private boolean identifierExists(@NotNull(message = "账号类型不能为空") IdentifierType identifierType, @NotBlank(message = "账号不能为空") String identifier) {
        return switch (identifierType) {
            case PHONE -> userService.existsByPhone(identifier);
            case EMAIL -> userService.existsByEmail(identifier);
        };
    }

    private Optional<AppUser> findUserById(long userId) {
        return userService.findById(userId);
    }

    private String normalizeIdentifier(IdentifierType identifierType, String identifier) {
        return switch (identifierType) {
            case EMAIL -> identifier.trim();
            case PHONE -> identifier.trim().toLowerCase();
        };
    }

    private void validateIdentifier(IdentifierType identifierType, String identifier) {
        if (identifierType == PHONE && !IdentifierValidator.isValidPhone(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }

        if (identifierType == EMAIL && !IdentifierValidator.isValidEmail(identifier)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
    }

    private Optional<Jwt> decodeRefreshTokenSafely(String refreshToken) {
        try {
            return Optional.of(jwtService.decode(refreshToken));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private Jwt decodeRefreshToken(@NotBlank(message = "刷新令牌不能为空") String refreshToken) {
        try {
            return jwtService.decode(refreshToken);
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    private Optional<AppUser> findUserByIdentifier(@NotNull(message = "账号类型不能为空") IdentifierType identifierType, @NotBlank(message = "账号不能为空") String identifier) {
        return switch (identifierType) {
            case PHONE -> userService.findByPhone(identifier);
            case EMAIL -> userService.findByEmail(identifier);
        };
    }
}
