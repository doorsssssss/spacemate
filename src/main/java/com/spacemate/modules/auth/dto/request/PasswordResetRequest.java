package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordResetRequest(
    @NotNull(message = "璐﹀彿绫诲瀷涓嶈兘涓虹┖") IdentifierType identifierType,
    @NotBlank(message = "璐﹀彿涓嶈兘涓虹┖") String identifier,
    @NotBlank(message = "楠岃瘉鐮佷笉鑳戒负绌?") String code,
    @NotBlank(message = "鏂板瘑鐮佷笉鑳戒负绌?") String newPassword
) {
}
