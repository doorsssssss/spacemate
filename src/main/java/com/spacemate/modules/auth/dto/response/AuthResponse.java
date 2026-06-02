package com.spacemate.modules.auth.dto.response;

public record AuthResponse(
        AuthUserResponse user,
        TokenResponse token
) {
}

