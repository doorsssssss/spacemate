package com.spacemate.modules.auth.service;

import com.spacemate.modules.auth.dto.request.PasswordResetRequest;
import com.spacemate.modules.auth.dto.request.RegisterRequest;
import com.spacemate.modules.auth.dto.request.TokenRefreshRequest;
import com.spacemate.modules.auth.dto.response.AuthResponse;
import com.spacemate.modules.auth.dto.response.AuthUserResponse;
import com.spacemate.modules.auth.dto.response.TokenResponse;
import com.spacemate.domain.entity.ClientInfo;
import com.spacemate.modules.auth.dto.request.LoginRequest;
import com.spacemate.modules.auth.dto.request.SendCodeRequest;
import com.spacemate.modules.auth.dto.response.SendCodeResponse;
import jakarta.validation.Valid;

public interface AuthService {

    AuthResponse register(@Valid RegisterRequest request, ClientInfo clientInfo );

    SendCodeResponse sendCode(SendCodeRequest request);

    AuthResponse login(LoginRequest request, ClientInfo clientInfo);

    TokenResponse refresh(TokenRefreshRequest request);

    void logout(String refreshToken);

    void resetPassword(PasswordResetRequest request);

    AuthUserResponse me(long userId);

}


