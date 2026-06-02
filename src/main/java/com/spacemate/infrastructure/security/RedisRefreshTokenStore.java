package com.spacemate.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * 鍩轰簬 Redis 鐨勫埛鏂颁护鐗岀櫧鍚嶅崟瀛樺偍銆?
 * <p>
 * 閿┖闂达細`auth:rt:{userId}:{tokenId}`锛屽€煎浐瀹氫负 "1"锛岃缃?TTL 鎺у埗杩囨湡銆?
 * 鏀寔鏍￠獙浠ょ墝鏈夋晥鎬с€佹挙閿€鍗曚釜浠ょ墝鎴栨挙閿€鏌愮敤鎴峰叏閮ㄤ护鐗屻€?
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore{
    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 灏嗗埛鏂颁护鐗屽啓鍏ョ櫧鍚嶅崟锛岃缃繃鏈熸椂闂淬€?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID銆?
     * @param ttl     鐢熷瓨鏃堕棿锛圧edis TTL锛夈€?
     */
    @Override
    public void storeToken(long userId, String tokenId, Duration ttl) {
        String key=key(userId,tokenId);
        //鎶婇€氳繃userid鍜宼okenid鐢熸垚鐨刱ey 瀛樺湪redis 鐒跺悗鈥?鈥濊〃鏄庢湭杩囨湡 瀵瑰簲 SETEX auth:rt:10086:abc12345 604800 "1"
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    /**
     * 鍒ゆ柇鍒锋柊浠ょ墝鏄惁浠嶆湁鏁堛€?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID銆?
     * @return 鏄惁鏈夋晥锛堥敭瀛樺湪涓斿€间负 "1"锛夈€?
     */
    @Override
    public boolean isTokenValid(long userId, String tokenId) {
        String key=key(userId,tokenId);
        return Objects.equals("1", redisTemplate.opsForValue().get(key));
    }

    /**
     * 鎾ら攢鍗曚釜鍒锋柊浠ょ墝銆?
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID锛坖ti锛夈€?
     */
    @Override
    public void revokeToken(long userId, String tokenId) {
        redisTemplate.delete(key(userId,tokenId));
    }

    /**
     * 鎾ら攢璇ョ敤鎴峰叏閮ㄥ埛鏂颁护鐗屻€?
     *
     * @param userId 鐢ㄦ埛 ID銆?
     */
    @Override
    public void revokeAll(long userId) {
        String pattern = "auth:rt:%d:*".formatted(userId);
        var keys = redisTemplate.keys(pattern);
        if(!keys.isEmpty()){
            redisTemplate.delete(keys);
        }
    }

    /**
     * 鐢熸垚鐧藉悕鍗曢敭鍚嶃€?
     *
     * @param userId  鐢ㄦ埛 ID銆?
     * @param tokenId 鍒锋柊浠ょ墝 ID銆?
     * @return Redis 閿悕銆?
     */
    private static String key(long userId, String tokenId) {
        //閫氳繃userid涓巘okenid鐢熸垚key
        //formatted鍏跺疄灏辨槸string鐨勪竴涓嚱鏁?寰€鍚庤拷鍔犲唴瀹?
        return "auth:rt:%d:%s".formatted(userId, tokenId);
    }
}

