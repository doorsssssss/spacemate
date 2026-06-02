package com.spacemate.config;

import io.jsonwebtoken.Jwt;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * 璁よ瘉鐩稿叧閰嶇疆灞炴€э紝缁戝畾鍓嶇紑 {@code auth.*}銆?
 *
 * <p>鍖呭惈浠ヤ笅鍒嗙粍锛?/p>
 * - Jwt锛氫护鐗岀鍙戜笌楠岃瘉閰嶇疆锛?
 * - Verification锛氶獙璇佺爜鍙戦€佷笌鏍￠獙閰嶇疆锛?
 * - Password锛氬瘑鐮佺瓥鐣ヤ笌鍔犲瘑寮哄害閰嶇疆銆?
 */
@Data
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    /** JWT 閰嶇疆椤广€?*/
    private final Jwt jwt = new Jwt();
    /** 楠岃瘉鐮侀厤缃」銆?*/
    private final Verification verification = new Verification();
    /** 瀵嗙爜绛栫暐閰嶇疆椤广€?*/
    private final Password password = new Password();

    @Data
    public static class Jwt {
        /** JWT 绛惧彂鑰呮爣璇嗭紙iss锛夈€?*/
        private String issuer = "SPACE_MATE";
        /** 璁块棶浠ょ墝鏈夋晥鏈燂紙TTL锛夈€?*/
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        /** 鍒锋柊浠ょ墝鏈夋晥鏈燂紙TTL锛夈€?*/
        private Duration refreshTokenTtl = Duration.ofDays(7);
        /** JWK 瀵嗛挜鏍囪瘑锛坘id锛夛紝鐢ㄤ簬涓嬫父鏍￠獙涓庤疆鎹€?*/
        private String keyId = "SPACE_MATE_KEY";
        /** RSA 绉侀挜 PEM锛圥KCS#8锛夎祫婧愩€?*/
        private Resource privateKey;
        /** RSA 鍏挜 PEM锛圶.509锛夎祫婧愩€?*/
        private Resource publicKey;
    }

    /**
     * 楠岃瘉鐮侀厤缃細浣嶆暟銆佹湁鏁堟湡銆佹渶澶у皾璇曟鏁般€佸彂閫侀棿闅斾笌姣忔棩涓婇檺銆?
     */
    @Data
    public static class Verification {
        /** 楠岃瘉鐮佷綅鏁般€?*/
        private int codeLength = 6;
        /** 楠岃瘉鐮佹湁鏁堟椂闂淬€?*/
        private Duration ttl = Duration.ofMinutes(5);
        /** 鏈€澶ф牎楠屽皾璇曟鏁般€?*/
        private int maxAttempts = 5;
        /** 鍚屾爣璇嗚繛缁彂閫佺殑鏈€灏忛棿闅斻€?*/
        private Duration sendInterval = Duration.ofSeconds(60);
        /** 鍚屾爣璇嗘瘡鏃ュ彂閫佷笂闄愩€?*/
        private int dailyLimit = 10;
    }

    /** 瀵嗙爜绛栫暐閰嶇疆銆?*/
    @Data
    public static class Password {
        /** 瀵嗙爜鍝堝笇寮哄害锛圔Crypt cost锛夈€?*/
        private int bcryptStrength = 12;
        /** 瀵嗙爜鏈€灏忛暱搴︺€?*/
        private int minLength = 8;
    }
}

