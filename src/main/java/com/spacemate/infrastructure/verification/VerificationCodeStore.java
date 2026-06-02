package com.spacemate.infrastructure.verification;

import com.spacemate.domain.entity.VerificationCheckResult;

import java.time.Duration;

/**
 * 楠岃瘉鐮佸瓨鍌ㄦ帴鍙ｃ€?
 * <p>
 * 鎶借薄楠岃瘉鐮佺殑淇濆瓨銆佹牎楠屼笌澶辨晥鎿嶄綔锛屽厑璁镐娇鐢?Redis 绛夊疄鐜般€?
 * 闇€鏀寔鏈€澶у皾璇曟鏁颁笌 TTL 浠ヤ繚璇佸畨鍏ㄦ€с€?
 */
public interface VerificationCodeStore {
    /**
     * 淇濆瓨楠岃瘉鐮併€?
     *
     * @param scene       鍦烘櫙鍚嶇О銆?
     * @param identifier  鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code        楠岃瘉鐮佸瓧绗︿覆銆?
     * @param ttl         鏈夋晥鏈熴€?
     * @param maxAttempts 鏈€澶у皾璇曟鏁般€?
     */
    void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts);

    /**
     * 鏍￠獙楠岃瘉鐮併€?
     *
     * @param scene      鍦烘櫙鍚嶇О銆?
     * @param identifier 鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code       鐢ㄦ埛杈撳叆鐨勯獙璇佺爜銆?
     * @return 鏍￠獙缁撴灉锛屽寘鍚姸鎬佷笌灏濊瘯娆℃暟缁熻銆?
     */
    VerificationCheckResult verify(String scene, String identifier, String code);

    /**
     * 浣块獙璇佺爜澶辨晥锛堝垹闄ゅ瓨鍌ㄨ褰曪級銆?
     *
     * @param scene      鍦烘櫙鍚嶇О銆?
     * @param identifier 鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     */
    void invalidate(String scene, String identifier);
}

