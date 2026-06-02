package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotNull(message = "鏍囪瘑绫诲瀷涓嶈兘涓虹┖") IdentifierType identifierType,
    @NotBlank(message = "鏍囪瘑鍊间笉鑳戒负绌?") String identifier,
    @NotBlank(message = "楠岃瘉鐮佷笉鑳戒负绌?") String code,
    String password,
    boolean agreeTerms
) {
}
