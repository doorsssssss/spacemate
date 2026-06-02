package com.spacemate.modules.auth.dto.request;

import com.spacemate.domain.enums.IdentifierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 閻ц缍嶇拠閿嬬湴閵?
 * <p>
 * 閺€顖涘瘮娑撱倗顫掑〒鐘讳壕閿?
 * - 妤犲矁鐦夐惍浣烘瑜版洩绱版繅顐㈠晸 `code`閿?
 * - 鐎靛棛鐖滈惂璇茬秿閿涙艾锝為崘?`password`閿涘牏鏁ら幋宄板嚒鐠佸墽鐤嗛弮璁圭礆閵?
 * `identifierType` 閹稿洤鐣剧拹锕€褰跨猾璇茬€烽敍鍫熷閺堝搫褰?闁喚顔堥敍澶涚礉`identifier` 娑撻缚澶勯崣宄扳偓绗衡偓?
 */
public record LoginRequest(
        @NotNull(message = "鐠愶箑褰跨猾璇茬€锋稉宥堝厴娑撹櫣鈹?") IdentifierType identifierType,
        @NotBlank(message = "鐠愶箑褰挎稉宥堝厴娑撹櫣鈹?") String identifier,
        String code,
        String password
) {
}

