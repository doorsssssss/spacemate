package com.spacemate.infrastructure.security;

import java.time.Duration;

/**
 * 鍒锋柊浠ょ墝鐧藉悕鍗曞瓨鍌ㄦ帴鍙ｃ€?
 * <p>
 * 璐熻矗绠＄悊 Refresh Token 鐨勬湁鏁堟€э細瀛樺偍銆佹牎楠屻€佹挙閿€鍗曚釜浠ょ墝涓庢挙閿€鐢ㄦ埛鍏ㄩ儴浠ょ墝銆?
 * 瀹炵幇鍙娇鐢?Redis銆佹暟鎹簱鎴栧叾瀹冩寔涔呭寲鏂规銆?
 */
public interface RefreshTokenStore {
    /**
     * 瀛樺偍鍒锋柊浠ょ墝鐧藉悕鍗曡褰曘€?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID锛坖ti锛夈€?
     * @param ttl     鐢熷瓨鏃堕棿锛堣繃鏈熷悗鑷姩澶辨晥锛夈€?
     */
    void storeToken(long userId, String tokenId, Duration ttl);

    /**
     * 鏍￠獙鍒锋柊浠ょ墝鏄惁浠嶇劧鏈夋晥锛堝湪鐧藉悕鍗曞唴涓旀湭杩囨湡锛夈€?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID锛坖ti锛夈€?
     * @return 鏄惁鏈夋晥銆?
     */
    boolean isTokenValid(long userId, String tokenId);

    /**
     * 鎾ら攢鎸囧畾鍒锋柊浠ょ墝锛堜粠鐧藉悕鍗曠Щ闄わ級銆?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID锛坖ti锛夈€?
     */
    void revokeToken(long userId, String tokenId);

    /**
     * 鎾ら攢鐢ㄦ埛鐨勬墍鏈夊埛鏂颁护鐗岋紙寮哄埗璇ョ敤鎴锋墍鏈変細璇濅笅绾匡級銆?
     *
     * @param userId 鐢ㄦ埛 ID銆?
     */
    void revokeAll(long userId);
}

