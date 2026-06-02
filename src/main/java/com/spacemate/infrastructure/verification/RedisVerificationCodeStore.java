package com.spacemate.infrastructure.verification;

import com.spacemate.domain.entity.VerificationCheckResult;
import com.spacemate.domain.enums.VerificationCodeStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * 鍩轰簬 Redis 鐨勯獙璇佺爜瀛樺偍瀹炵幇銆?
 * <p>
 * 浣跨敤 Hash 缁撴瀯淇濆瓨 `code`銆乣maxAttempts` 涓?`attempts`锛孴TL 鎺у埗鏈夋晥鏈熴€?
 * 鏍￠獙鏃舵敮鎸佸皾璇曡鏁颁笌閿欒鐘舵€佽繑鍥烇紝鎴愬姛鍚庡垹闄ら敭浠ラ槻閲嶇敤銆?
 */
@Component
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private static final String FIELD_CODE = "code";
    private static final String FIELD_MAX_ATTEMPTS = "maxAttempts";
    private static final String FIELD_ATTEMPTS = "attempts";

    private final StringRedisTemplate redisTemplate;

    public RedisVerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 淇濆瓨楠岃瘉鐮佸埌 Redis Hash锛屽苟璁剧疆 TTL銆?
     *
     * @param scene       鍦烘櫙鍚嶇О銆?
     * @param identifier  鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code        楠岃瘉鐮佸瓧绗︿覆銆?
     * @param ttl         鏈夋晥鏈熴€?
     * @param maxAttempts 鏈€澶у皾璇曟鏁般€?
     * @throws RedisSystemException 淇濆瓨澶辫触鏃舵姏鍑恒€?
     */
    @Override
    public void saveCode(String scene, String identifier, String code, Duration ttl, int maxAttempts) {
        String key=buildKey(scene,identifier);

        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();

        try {
            ops.put(key,FIELD_CODE,code);
            ops.put(key,FIELD_MAX_ATTEMPTS,String.valueOf(maxAttempts));
            ops.put(key, FIELD_ATTEMPTS, "0");
            redisTemplate.expire(key, ttl);
        }catch (DataAccessException ex){
            throw new RedisSystemException("Failed to save verification code", ex);
        }
    }

    /**
     * 鐢熸垚楠岃瘉鐮佺殑 Redis 閿悕銆?
     *
     * @param scene      鍦烘櫙鍚嶇О銆?
     * @param identifier 鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @return 閿悕瀛楃涓层€?
     */
    private String buildKey(String scene, String identifier) {
        return "auth:code:%s:%s".formatted(scene, identifier);
    }

    /**
     * 鏍￠獙楠岃瘉鐮佹槸鍚﹀尮閰嶏紝鏇存柊灏濊瘯璁℃暟骞跺湪鎴愬姛鏃跺垹闄よ褰曘€?
     *
     * @param scene      鍦烘櫙鍚嶇О銆?
     * @param identifier 鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     * @param code       鐢ㄦ埛杈撳叆鐨勯獙璇佺爜銆?
     * @return 鏍￠獙缁撴灉锛堟垚鍔熴€佹湭鎵惧埌銆侀敊璇€佸皾璇曡繃澶氾級銆?
     */
    @Override
    public VerificationCheckResult verify(String scene, String identifier, String code) {
        //鏍规嵁鏍囪瘑鍜屽満鏅緱鍒伴獙璇佺爜
        String key=buildKey(scene,identifier);

        //鑾峰緱redis鍏ㄩ儴hash
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        //鑾峰彇key鐨勯敭鍊煎
        Map<String, String> data = ops.entries(key);

        if(data.isEmpty()){//濡傛灉娌℃壘鍒?
            return new VerificationCheckResult(VerificationCodeStatus.NOT_FOUND, 0, 0);
        }
        String storeCode = data.get(FIELD_CODE);
        //鏈€澶у皾璇曟鏁?
        int maxAttempts = parseInt(data.get(FIELD_MAX_ATTEMPTS), 5);
        //宸插皾璇曟鏁?
        int attempts = parseInt(data.get(FIELD_ATTEMPTS), 0);
        if(attempts>=maxAttempts){//瓒呰繃鏈€澶у彲鎵ц娆℃暟
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, attempts, maxAttempts);
        }
        if(Objects.equals(storeCode,code)){
            redisTemplate.delete(key);
            return new VerificationCheckResult(VerificationCodeStatus.SUCCESS, attempts, maxAttempts);
        }
        int updatedAttempts = attempts + 1;
        ops.put(key,FIELD_ATTEMPTS,String.valueOf(updatedAttempts));
        if(updatedAttempts>=maxAttempts){//杈惧埌涓婇檺璁剧疆30鍒嗛挓杩囨湡
            redisTemplate.expire(key, Duration.ofMinutes(30));
            return new VerificationCheckResult(VerificationCodeStatus.TOO_MANY_ATTEMPTS, updatedAttempts, maxAttempts);
        }
        return new VerificationCheckResult(VerificationCodeStatus.MISMATCH, updatedAttempts, maxAttempts);
    }

    /**
     * 瑙ｆ瀽鏁存暟瀛楃涓诧紝澶辫触杩斿洖榛樿鍊笺€?
     *
     * @param value
     * @param defaultValue
     * @return
     */
    private  static int parseInt(String value, int defaultValue) {
        if(value==null){
            return defaultValue;
        }
        try{
            return Integer.parseInt(value);
        } catch (NumberFormatException ex){
            return defaultValue;
        }
    }

    /**
     *  浣块獙璇佺爜澶辨晥锛堝垹闄ゅ瓨鍌ㄨ褰曪級銆?
     *
     * @param scene      鍦烘櫙鍚嶇О銆?
     * @param identifier 鏍囪瘑锛堟墜鏈哄彿鎴栭偖绠憋級銆?
     */
    @Override
    public void invalidate(String scene, String identifier) {
        redisTemplate.delete(buildKey(scene,identifier));
    }

}

