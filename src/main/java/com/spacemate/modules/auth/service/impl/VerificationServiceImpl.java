package com.spacemate.modules.auth.service.impl;

import com.spacemate.common.exception.BusinessException;
import com.spacemate.config.AuthProperties;
import com.spacemate.common.error.ErrorCode;
import com.spacemate.domain.entity.SendCodeResult;
import com.spacemate.infrastructure.verification.CodeSender;
import com.spacemate.domain.entity.VerificationCheckResult;
import com.spacemate.domain.enums.VerificationScene;
import com.spacemate.modules.auth.service.VerificationService;
import com.spacemate.infrastructure.verification.VerificationCodeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {
    //閺囨潙鐣ㄩ崗銊ф畱闂呭繑婧€閺佹壆鏁撻幋鎰珤
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    //閸欐垿鐛欑拠浣虹垳閻ㄥ嫬婀撮弬?
    private final VerificationCodeStore codeStore;
    //閸欐垿鐛欑拠浣虹垳閻ㄥ嫬浼愰崗?
    private final CodeSender codeSender;
    private final StringRedisTemplate stringRedisTemplate;
    //鐠囪褰囬柊宥囩枂
    private final AuthProperties properties;

    /**
     * 閸欐垿鈧線鐛欑拠浣虹垳閸掔増瀵氱€规碍鐖ｇ拠鍡愨偓?
     * <p>
     * 閹笛嗩攽閸欐垿鈧線妫块梾鏂剧瑢閺冦儲顐奸弫浼存閸掕绱濋悽鐔稿灇闂呭繑婧€閺佹澘鐡ф宀冪槈閻緤绱濇穱婵嗙摠閸掓澘鐡ㄩ崒銊ヨ嫙鐠嬪啰鏁ら崣鎴︹偓浣告珤閵?
     *
     * @param scene      妤犲矁鐦夐惍浣告簚閺咁垽绱橰EGISTER/LOGIN/RESET_PASSWORD閿涘鈧?
     * @param identifier 閺嶅洩鐦戦敍鍫熷閺堝搫褰块幋鏍仏缁犳唻绱氶妴?
     * @return 閸欐垿鈧胶绮ㄩ弸婊愮礉閸栧懎鎯堥弽鍥槕閵嗕礁婧€閺咁垯绗屾潻鍥ㄦ埂缁夋帗鏆熼妴?
     * @throws BusinessException 閸欏倹鏆熸稉宥呯暚閺佸瓨鍨ㄧ憴锕€褰傞柅鐔哄芳/閺冦儵妾烘０婵囨閹舵稑鍤妴?
     */
    @Override
    public SendCodeResult sendCode(VerificationScene scene, String identifier) {
        if(scene==null||!StringUtils.hasText(identifier)){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "鐠囬攱褰佹笟娑欘劀绾喚娈戞宀冪槈閻礁褰傞柅浣稿棘閺?");
        }

        AuthProperties.Verification cfg=properties.getVerification();
        enforceSendInterval(scene,identifier,cfg.getSendInterval());
        enforceDailyLimit(scene,identifier,cfg.getDailyLimit());

        String code=generateNumericCode(cfg.getCodeLength());
        codeStore.saveCode(scene.name(),identifier,code,cfg.getTtl(),cfg.getMaxAttempts());
        codeSender.sendCode(scene,identifier,code,(int)cfg.getTtl().toMinutes());
        return new SendCodeResult(identifier,scene,(int)cfg.getTtl().toSeconds());
    }

    /**
     * 娴ｅ潡鐛欑拠浣虹垳婢惰鲸鏅ラ敍鍫濆灩闂勩倕鐡ㄩ崒銊唶瑜版洩绱氶妴?
     *
     * @param scene      妤犲矁鐦夐惍浣告簚閺咁垬鈧?
     * @param identifier 閺嶅洩鐦戦敍鍫熷閺堝搫褰块幋鏍仏缁犳唻绱氶妴?
     */
    @Override
    public void invalidate(VerificationScene scene, String identifier) {
        codeStore.invalidate(scene.name(), identifier);
    }

    /**
     * 閺嶏繝鐛欐宀冪槈閻焦妲搁崥锔筋劀绾喕绗栭張顏囩Т闂勬劑鈧?
     *
     * @param scene      妤犲矁鐦夐惍浣告簚閺咁垬鈧?
     * @param identifier 閺嶅洩鐦戦敍鍫熷閺堝搫褰块幋鏍仏缁犳唻绱氶妴?
     * @param code       閻劍鍩涙潏鎾冲弳閻ㄥ嫰鐛欑拠浣虹垳閵?
     * @return 閺嶏繝鐛欑紒鎾寸亯閿涘苯瀵橀崥顐ゅЦ閹椒绗岀亸婵婄槸濞嗏剝鏆熺紒鐔活吀閵?
     * @throws BusinessException 閸欏倹鏆熸稉宥呯暚閺佸瓨妞傞幎娑樺毉閵?
     */
    @Override
    public VerificationCheckResult verify(VerificationScene scene, String identifier, String code) {
        if(scene==null||!StringUtils.hasText(identifier)){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "妤犲矁鐦夐惍浣圭墡妤犲苯寮弫棰佺瑝鐎瑰本鏆?");
        }
        return codeStore.verify(scene.name(),identifier,code);
    }

    /**
     * 濮ｅ繑妫╅崣鎴︹偓浣诡偧閺佷即妾洪崚璁圭窗鐡掑懓绻冩稉濠囨閸掓瑦濮忛崙娲妫版繂绱撶敮鎼炩偓?
     *
     * @param scene      妤犲矁鐦夐惍浣告簚閺咁垬鈧?
     * @param identifier 閺嶅洩鐦戦敍鍫熷閺堝搫褰块幋鏍仏缁犳唻绱氶妴?
     * @param limit      濮ｅ繑妫╂稉濠囨濞嗏剝鏆熼妴?
     */
    private void enforceDailyLimit(VerificationScene scene, String identifier, int limit) {
        if(limit<=0){
            return;
        }
        String date=DAY_FORMAT.format(LocalDate.now());
        String key="auth:code:count"+scene.name()+":"+identifier+":"+date;
        Long count=stringRedisTemplate.opsForValue().increment(key);
        if(count!=null&&count==1L){
            stringRedisTemplate.expire(key, Duration.ofDays(1));
        }

        if(count!=null&&count>limit){
            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
        }
    }

    /**
     * 閸欐垿鈧線妫块梾鏃堟閸掕绱伴崥灞肩閺嶅洩鐦戦崷銊﹀瘹鐎规岸妫块梾鏂垮敶閸欘亣鍏橀崣鎴︹偓浣风濞喡扳偓?
     *
     * @param scene      妤犲矁鐦夐惍浣告簚閺咁垬鈧?
     * @param identifier 閺嶅洩鐦戦敍鍫熷閺堝搫褰块幋鏍仏缁犳唻绱氶妴?
     * @param interval   閸欐垿鈧線妫块梾鏂烩偓?
     */
    private void enforceSendInterval(VerificationScene scene, String identifier, Duration interval) {
        if(interval.isZero()||interval.isNegative()){
            return;
        }
        //鐠佹澘缍嶉幍瀣簚閸欓攱娓堕崥搴濈濞嗏€冲絺妤犲矁鐦夐惍浣烘畱閺冨爼妫?
        String key="auth:code:last:"+scene.name()+":"+identifier;
        String existing=stringRedisTemplate.opsForValue().get(key);
        if(existing!=null){
            throw new BusinessException(ErrorCode.VERIFICATION_RATE_LIMIT);
        }
        stringRedisTemplate.opsForValue().set(key, "1", interval);
    }

    /**
     * 閻㈢喐鍨氶幐鍥х暰闂€鍨閻ㄥ嫮鍑介弫鏉跨摟妤犲矁鐦夐惍浣碘偓?
     *
     * @param length 妤犲矁鐦夐惍渚€鏆辨惔锔衡偓?
     * @return 閺佹澘鐡х€涙顑佹稉灞傗偓?
     */
    private String generateNumericCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for(int i=0;i<length;i++){
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

}


