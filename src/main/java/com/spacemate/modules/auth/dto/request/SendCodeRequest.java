package com.spacemate.modules.auth.dto.request;


import com.spacemate.domain.enums.IdentifierType;
import com.spacemate.domain.enums.VerificationScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 閸欐垿鈧線鐛欑拠浣虹垳鐠囬攱鐪伴妴?
 * <p>
 * `scene` 閹稿洤鐣鹃崷鐑樻珯閿涘牊鏁為崘?閻ц缍?闁插秶鐤嗙€靛棛鐖滈敍澶涚礉闁板秴鎮庣拹锕€褰跨猾璇茬€锋稉搴♀偓鑲╂暏娴滃海鏁撻幋鎰嫙閸欐垿鈧線鐛欑拠浣虹垳閵?
 */
public record SendCodeRequest(
        @NotNull(message = "閸︾儤娅欐稉宥堝厴娑撹櫣鈹?") VerificationScene scene,
        @NotNull(message = "鐠愶箑褰跨猾璇茬€锋稉宥堝厴娑撹櫣鈹?") IdentifierType identifierType,
        @NotBlank(message = "鐠愶箑褰挎稉宥堝厴娑撹櫣鈹?") String identifier
) {
}

