package com.spacemate.modules.auth.controller;

import com.spacemate.modules.auth.dto.request.LogoutRequest;
import com.spacemate.modules.auth.dto.request.PasswordResetRequest;
import com.spacemate.modules.auth.dto.request.RegisterRequest;
import com.spacemate.modules.auth.dto.request.TokenRefreshRequest;
import com.spacemate.modules.auth.dto.response.AuthResponse;
import com.spacemate.modules.auth.dto.response.AuthUserResponse;
import com.spacemate.modules.auth.dto.response.TokenResponse;
import com.spacemate.domain.entity.ClientInfo;
import com.spacemate.modules.auth.dto.request.LoginRequest;
import com.spacemate.modules.auth.dto.request.SendCodeRequest;
import com.spacemate.modules.auth.dto.response.SendCodeResponse;
import com.spacemate.modules.auth.service.AuthService;
import com.spacemate.modules.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 鍙戦€佺煭淇?閭楠岃瘉鐮併€?
     * <p>
     * 鏍规嵁鍦烘櫙锛堟敞鍐屻€佺櫥褰曘€侀噸缃瘑鐮侊級鍚戞寚瀹氭爣璇嗭紙鎵嬫満鍙锋垨閭锛夊彂閫佷竴娆℃€ч獙璇佺爜銆?
     *
     * @param request 璇锋眰浣擄紝鍖呭惈锛?
     *                - identifierType锛氭爣璇嗙被鍨嬶紝PHONE 鎴?EMAIL锛?
     *                - identifier锛氭墜鏈哄彿鎴栭偖绠卞湴鍧€锛?
     *                - scene锛氶獙璇佺爜浣跨敤鍦烘櫙锛圧EGISTER/LOGIN/RESET_PASSWORD锛夈€?
     * @return 鍝嶅簲浣擄紝鍖呭惈鐩爣鏍囪瘑銆佸満鏅互鍙婇獙璇佺爜杩囨湡绉掓暟銆?
     */
    @PostMapping("/send-code")
    public SendCodeResponse sendCode(@Valid @RequestBody SendCodeRequest request){
        return authService.sendCode(request);
    }

    /**
     * 娉ㄥ唽鏂扮敤鎴峰苟鑷姩鐧诲綍銆?
     * <p>
     * 楠岃瘉鏍囪瘑涓庨獙璇佺爜鍚庡垱寤虹敤鎴凤紝鑻ユ彁渚涘瘑鐮佸垯杩涜澶嶆潅搴︽牎楠屽苟淇濆瓨瀵嗙爜鍝堝笇锛涙垚鍔熷悗绛惧彂 Access/Refresh Token銆?
     *
     * @param request     璇锋眰浣擄紝鍖呭惈锛氭爣璇嗙被鍨嬩笌鍊笺€侀獙璇佺爜銆佸彲閫夊瘑鐮併€佹槸鍚﹀悓鎰忓崗璁€?
     * @param httpRequest 鐢ㄤ簬瑙ｆ瀽瀹㈡埛绔俊鎭紙IP 涓?User-Agent锛夛紝璁板綍瀹¤鏃ュ織銆?
     * @return 璁よ瘉鍝嶅簲锛屽寘鍚敤鎴蜂俊鎭笌浠ょ墝瀵广€?
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,HttpServletRequest httpRequest){
        return authService.register(request,resolveClient(httpRequest));
    }

    /**
     * 鐧诲綍骞惰幏鍙栦护鐗屽銆?
     * <p>
     * 鏀寔涓ょ閫氶亾锛氬瘑鐮佺櫥褰曟垨楠岃瘉鐮佺櫥褰曪紱鎴愬姛鍚庣鍙?Access/Refresh Token銆?
     *
     * @param request     璇锋眰浣擄紝鍖呭惈锛氭爣璇嗙被鍨嬩笌鍊笺€佸瘑鐮佹垨楠岃瘉鐮侊紙浜岄€変竴锛夈€?
     * @param httpRequest 鐢ㄤ簬瑙ｆ瀽瀹㈡埛绔俊鎭紙IP 涓?User-Agent锛夛紝璁板綍瀹¤鏃ュ織銆?
     * @return 璁よ瘉鍝嶅簲锛屽寘鍚敤鎴蜂俊鎭笌浠ょ墝瀵广€?
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, resolveClient(httpRequest));
    }

    /**
     * 浣跨敤 Refresh Token 鍒锋柊浠ょ墝銆?
     * <p>
     * 鏍￠獙鍒锋柊浠ょ墝鐨勫悎娉曟€т笌鐧藉悕鍗曠姸鎬侊紝绛惧彂鏂扮殑浠ょ墝瀵癸紝骞舵挙閿€鏃у埛鏂颁护鐗屻€?
     *
     * @param request 璇锋眰浣擄紝鍖呭惈锛歳efreshToken锛堝埛鏂颁护鐗岋級銆?
     * @return 鏂扮殑浠ょ墝鍝嶅簲锛坅ccessToken/refreshToken 鍙婂叾杩囨湡鏃堕棿锛夈€?
     */
    @PostMapping("/token/refresh")
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    /**
     * 鐧诲嚭骞舵挙閿€鍒锋柊浠ょ墝銆?
     * <p>
     * 鑻ユ彁渚涚殑浠ょ墝涓哄悎娉曠殑 Refresh Token锛屽垯鎾ら攢鍏剁櫧鍚嶅崟璁板綍锛涜繑鍥?204锛屾棤鍝嶅簲浣撱€?
     *
     * @param request 璇锋眰浣擄紝鍖呭惈锛歳efreshToken锛堟鎾ら攢鐨勫埛鏂颁护鐗岋級銆?
     * @return 绌哄搷搴旓紝HTTP 204 No Content銆?
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * 浣跨敤楠岃瘉鐮侀噸缃瘑鐮併€?
     * <p>
     * 楠岃瘉鏍囪瘑涓庨獙璇佺爜鍚庢洿鏂扮敤鎴峰瘑鐮佸搱甯岋紝骞舵挙閿€璇ョ敤鎴锋墍鏈夊埛鏂颁护鐗屼互寮哄埗涓嬬嚎銆?
     *
     * @param request 璇锋眰浣擄紝鍖呭惈锛氭爣璇嗙被鍨嬩笌鍊笺€侀獙璇佺爜銆佹柊瀵嗙爜銆?
     * @return 绌哄搷搴旓紝HTTP 204 No Content銆?
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 鏌ヨ褰撳墠鐧诲綍鐢ㄦ埛淇℃伅銆?
     * <p>
     * 鍩轰簬 Spring Security 娉ㄥ叆鐨?`Jwt` 浠ょ墝锛屾彁鍙栫敤鎴?ID 骞惰繑鍥炵敤鎴锋瑕佷俊鎭€?
     *
     * @param jwt 褰撳墠璇锋眰缁戝畾鐨?JWT 浠ょ墝锛堟潵鑷?`Authorization: Bearer`锛夈€?
     * @return 鐢ㄦ埛淇℃伅鍝嶅簲銆?
     */
    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return authService.me(userId);
    }


    private ClientInfo resolveClient(HttpServletRequest httpRequest) {
        String ip=extractClientIp(httpRequest);
        String ua=httpRequest.getHeader("User-Agent");
        return new ClientInfo(ip,ua);
    }

    private String extractClientIp(HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");

        if(ip!=null && !ip.isBlank()){
            return ip.split(",")[0].trim();
        }
        String realIp = httpRequest.getHeader("X-Real-IP");
        if(realIp!=null && !realIp.isBlank()){
            return realIp.split(",")[0].trim();
        }
        return httpRequest.getRemoteAddr();
    }


}


