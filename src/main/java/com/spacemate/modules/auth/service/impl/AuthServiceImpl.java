package com.spacemate.modules.auth.service.impl;

import com.spacemate.common.exception.BusinessException;
import com.spacemate.config.AuthProperties;
import com.spacemate.domain.entity.*;
import com.spacemate.common.error.ErrorCode;
import com.spacemate.domain.enums.IdentifierType;
import com.spacemate.domain.enums.VerificationCodeStatus;
import com.spacemate.domain.enums.VerificationScene;
import com.spacemate.modules.auth.dto.response.SendCodeResponse;
import com.spacemate.modules.auth.dto.request.*;
import com.spacemate.modules.auth.dto.response.AuthResponse;
import com.spacemate.modules.auth.dto.response.AuthUserResponse;
import com.spacemate.modules.auth.dto.response.TokenResponse;
import com.spacemate.modules.auth.service.*;
import com.spacemate.infrastructure.security.RefreshTokenStore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.spacemate.domain.enums.IdentifierType.EMAIL;
import static com.spacemate.domain.enums.IdentifierType.PHONE;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties authProperties;
    private final VerificationService verificationService;
    private final UserService userService;

    @Override
    public AuthResponse register(RegisterRequest request, ClientInfo clientInfo) {
       if(!request.agreeTerms()){//婵犵數鍋涙径鍥焵椤掑啯鐝柛濠囦憾閺岀喖寮剁捄銊ь唶閻庤娲橀〃鍫ュ箯?
           throw new BusinessException(ErrorCode.TERMS_NOT_ACCEPTED);
       }
       validateIdentifier(request.identifierType(), request.identifier());

       String identifier = normalizeIdentifier(request.identifierType(), request.identifier());

       if(identifierExists(request.identifierType(),request.identifier())){
           throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
       }
        ensureVerificationSuccess(verificationService.verify(VerificationScene.REGISTER, identifier, request.code()));

       AppUser appUser =new AppUser();
       appUser.setPhone(request.identifierType()== PHONE?identifier:null);
       appUser.setEmail(request.identifierType()== EMAIL?identifier:null);
       appUser.setNickname(generateNickname());
       appUser.setBio(null);
       appUser.setAvatar(null);//濠电偞鍨堕幐鎼佹晝閿濆洦顫曢柤鎼佹涧缁剁偤鏌℃径瀣仴闁?
       appUser.setTagsJson("[]");

       if(StringUtils.hasText(request.password())){
           validatePassword(request.password());
           appUser.setPasswordHash(passwordEncoder.encode(request.password().trim()));
       }
       userService.createUser(appUser);
       TokenPair tokenPair=jwtService.issueTokenPair(appUser);
       storeRefreshToken(appUser.getId(), tokenPair);
        loginLogService.record(appUser.getId(), identifier, "REGISTER", clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");

        return new AuthResponse(mapUser(appUser), mapToken(tokenPair));
    }

    @Override
    public SendCodeResponse sendCode(SendCodeRequest request) {
        //濠碘槅鍋撶徊楣冩偋閺囥垹鍌ㄦ繛鎴烆焸濞戙垹鐒垫い鎺戝閽冪喖鏌曟径娑橆洭濠㈣鍔楅埀?
        validateIdentifier(request.identifierType(), request.identifier());
        //闂備礁鎼粔鏉懨洪妶澶婇棷妞ゆ牜鍋涚粈宀勬煛瀹ュ啫濡块柛銊ャ偢閹綊宕惰椤ョ偤鏌＄€ｎ亜鏆ｇ€?
        String normalized = normalizeIdentifier(request.identifierType(), request.identifier());

        boolean exists = identifierExists(request.identifierType(), request.identifier());

        if (request.scene() == VerificationScene.REGISTER && exists) {
            //闂備礁婀辩划顖炲礉濡ゅ懎桅婵椴哥€氬鏌ㄩ弴妤€浜鹃悷婊勬緲閻忔繈顢欒箛娑樼闁圭儤姊归幉濂告⒑?
            throw new BusinessException(ErrorCode.IDENTIFIER_EXISTS);
        }
        if ((request.scene() == VerificationScene.LOGIN || request.scene() == VerificationScene.RESET_PASSWORD) && !exists) {
            //闂佽绻愮换妤併仈濮濆被浜瑰ù锝堟閻棗螖閿曗偓閻忔繈藝閳哄懏鐓曟繛鍡樻尰閺佸崬顭跨憴鍕嗘垹绮欐径濠庡悑闁告侗鍘鹃崢?
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        SendCodeResult result = verificationService.sendCode(request.scene(), normalized);
        return new SendCodeResponse(result.identifier(), result.scene(), result.expireSeconds());

    }

    /**
     * 闂備浇鐨崱鈺佹闂佹悶鍊ら崣鍐极瀹ュ拋娼╂い鎾跺枎椤曨亪姊绘担鍝ユ皑闁稿鎹囬弻鐔虹矙閹稿孩鎮欓梺浼欑秮缁犳牕鐣峰鑸电叆閻庯綆鍋嗛崢鎰箾鐎电袨闁稿﹥顨嗛弲璺衡攽鐎ｎ€?
     *
     * @param refreshToken 闂備礁鎲＄敮锟犲绩闁秴钃熷┑鐘插暟椤╃兘鏌曢崼婵囨悙妞ゃ倕鍊块幃妤€鈽夊▎妯荤暭濡炪倖鍨抽悞锔剧矙婢跺本瀚氱憸蹇涚嵁閵忋倖鐓犳慨鐟版搐閸斻儳鎮▎鎾寸厸閻庯綆鍋勬慨鍐偣閹邦喖鏋涚€规洏鍎靛畷濂告偄閹巻鍋撴繝鍥ㄧ厱闁圭儤顨嗛弫閬嶆煛鐏炴枻鏀荤紒鍌涘浮婵″爼宕惰閺夋椽姊洪崨濠傜瑲闁荤噦绠撻獮鎰枎閹惧鍙嗛梺閫炲苯澧寸€规洘顨婂畷婊勬媴鐠団€叉闂備礁鎲￠懝鍓х矓閺夋垹鏆﹂柣鏃傚劋婵ジ鏌℃径搴㈢《缂佸娼￠弻?
     */
    public void logout(String refreshToken){
        decodeRefreshTokenSafely(refreshToken).ifPresent(jwt->{
            if(Objects.equals("refresh",jwtService.extractTokenType(jwt))){
                long userId=jwtService.extractUserId(jwt);
                String tokenId=jwtService.extractTokenId(jwt);
                refreshTokenStore.revokeToken(userId,tokenId);
            }
        });
    }

    /**
     * 闂備礁鎼悮顐﹀磿閹绢噮鏁嬫俊銈呮噺閸嬨劑鏌曟繝蹇曠暠闁绘挻娲橀幈銊╁箳閹存績鍋撴禒瀣ㄢ偓鍛存晜閻ｅ备鏀抽梺鏂ユ櫅閸熲晝妲愰弽顓熺厪?
     *
     * @param userId 闂備焦妞垮鍧楀礉瀹ュ鏄?ID闂?
     * @return 闂備焦妞垮鍧楀礉瀹ュ鏄ユ繛鎴烇供濞硷繝鏌涚仦鍓х婵炶缍侀弻娑樷攽閸℃銏㈢磼閵婏妇鍙€濠?
     * @throws BusinessException 闁荤喐绮庢晶妤呭箰妤ｅ啫鏋侀柕鍫濐槸缁狅綁鏌у顒€鈧鎮峰┑瀣€垫繛鎴烆仾椤忓嫸鑰挎い蹇撶墕缁秹鏌ら幖浣规锭妞ゎ偅妫冮弻娑㈠箣閻愭媽鍩呴梺?
     */
    public AuthUserResponse me(long userId) {
        AppUser user = findUserById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        return mapUser(user);
    }


    /**
     * 闂備浇鐨崱鈺佹缂傚倸绉寸粔褰掓偘椤曗偓瀹曟粍绗熼崶褍袨闂備礁鎲￠悷锕傚垂閹惰棄绠柕鍫濐槹閸嬪鎮橀悙闈涘姦闁?
     * <p>
     * 闂備浇銆€閸嬫挻銇勯弽銊р槈闁伙富鍣ｉ幃妤呮濞戞瑯妫ら梺缁樼墬缁捇鐛€ｎ喖鍐€妞ゆ挾濮撮惌妤呮煟閻樺弶鐭楅柤鍐茬埣閹儱顭ㄩ崼鐔哄姷濠殿喗蓱濠€瑙勭珶閺囥垺鐓ユ繛鎴炵懅閳洟鏌涢妸锕€鍝虹€规洘绻堥幃銏ゆ煥鐎ｎ亖鍋撻悽鍛婂仯濞达綀顫夌欢鑼磽瀹ュ懐肖闁逞屽墮椤掓繈濡舵惔鈩冨€庨梻浣瑰缁嬫垿鎯夋總鍓叉晢闁绘垼妫勯惌妤呮煙鐎涙鐭嬪┑顔哄灲閺岋絽螖閳ь剟鎳熼鐑嗘晪妞ゆ洍鍋撻柣娑卞櫍瀵敻鎮介悽娈垮晬闂佽瀛╃粙鎺楁晪闂佺硶鏅滈惄顖氼嚕椤掍緤绱ｅù锝囨嚀琚橀梻浣虹帛椤ㄥ懘鎯夐悡搴樻瀺鐟滄柨鐣烽妷銉悑闁告侗鍘鹃ˇ浼存⒑?
     *
     * @param request    闂備浇鐨崱鈺佹缂傚倸绉寸粔鍫曞箯閻樼粯鐓ラ悗锝庡墯閸曢箖姊洪幐搴ｂ槈闁兼椿鍨抽埀顒€鐏氶敃銏犵暦閵夆晩鏁冩い鎺戝€婚悰銉╂⒑閸濆嫮孝婵炴祴鏅犻幆渚€骞嬮悩闈涙殫闁荤姴娲╃亸娆撳焵椤掑嫭鏁辩紒瀣槹閹棃鍩﹂埀顒勫磻閹惧墎绠旈悶娑掆偓鍏呭婵炶揪绲介幗婊兾ｉ弴銏＄厽妞ゆ棁宕甸崝宥夋煕閵婏絽濡芥い銊ｅ劚閻ｆ繈宕橀鍜佹綌闂備焦妞块崰鏇犳閵堝洦顫曟繛鍡樺姉椤╄尙鈧厜鍋撻柛鏇樺妼娴滄儳顭跨捄渚Ш妞ゅ繘浜堕弻銊モ槈濮楀棙肖闂?
     * @param clientInfo 闂佽楠哥粻宥夊垂濞差亜鏄ユ繛鎴炴皑閸楁碍銇勯弽銊ф噥缂佽妫濋弻鐔碱敇瑜嶉悘鑼磼鏉堛劉鏀/UA闂備焦瀵х粙鎴βㄩ埀顒傜磼鏉堛劎绠為柟顔荤矙婵℃悂濡堕崶顏勵棟闂備浇鐨崱鈺佹缂傚倸绉寸粔鍫曞焵椤掆偓椤掓繈濡舵惔鈩冨€庨梻?
     * @return 闂佽崵濮抽梽宥夊磹濞戙垺鍎婂鑸靛姇娴肩姷鈧箍鍎遍幊鎰偓鐟邦樀閺屻劌鈽夊Ο鍨伃闁诲骸鐏氶敃銏犵暦閵夆晩鏁冮柕鍫濇濞堛垽姊洪崷顓ф婵炴捁顫夌粚杈ㄧ節閸パ呯暢濡炪倖鐗楅悷褔鎮烽姀鐘嗗綊鎮╅灏栧亾濡や焦娅犳繝闈涚墐閸嬫捇妫冨☉姘辨殯闂?
     * @throws BusinessException 闁荤喐绮庢晶妤呭箰妤ｅ啫鏋侀柕鍫濐槸缁狅綁鏌у顒€鈧鎮峰┑瀣€垫繛鎴烆仾椤忓嫸鑰挎い蹇撶墕杩濇繛杈剧悼椤牆鈻撻崼鏇熷仯闁搞儻濡囩粣鏃堟煛娴ｉ潻鍔熼柟椋庡█椤㈡稑鈽夊Ο闂寸玻闂佽崵濮村ú顓㈠绩闁秵鍎戦柣妤€鐗忛埢鏃傗偓骞垮劚閹冲繘宕愰妶鍡欑濠㈣泛顑嗙粈鍐╀繆閻愭彃鈧潡鐛惔鈾€妲堟俊顖滃劋閻︽棃姊?
     */
    public AuthResponse login(LoginRequest request, ClientInfo clientInfo){
        validateIdentifier(request.identifierType(), request.identifier());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        Optional<AppUser> userOptional = findUserByIdentifier(request.identifierType(), request.identifier());
        if(userOptional.isEmpty()){
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        AppUser user = userOptional.get();
        String channel;

        if(StringUtils.hasText(request.password())){
            channel="PASSWORD";
            if(!StringUtils.hasText(user.getPasswordHash())||!passwordEncoder.matches(request.password(),user.getPasswordHash())){
                loginLogService.record(user.getId(), identifier,channel,clientInfo.ip(),clientInfo.userAgent(),"FAILED");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        }else if(StringUtils.hasText(request.code())){
            channel="CODE";
            ensureVerificationSuccess(verificationService.verify(VerificationScene.LOGIN,identifier,request.code()));
        }else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "闂佽崵濮村ú顓㈠绩鏉堫偁浜瑰ù锝呮贡閻熻绻涢幋娆忕伄鐎规洩缍侀幃褰掑炊閿曚礁鍘￠梺缁樼墬缁捇鐛€ｎ喖鍐€鐟滃繐危閺囥垺鐓?");
        }
        TokenPair tokenPair=jwtService.issueTokenPair(user);
        storeRefreshToken(user.getId(),tokenPair);
        loginLogService.record(user.getId(), identifier, channel, clientInfo.ip(), clientInfo.userAgent(), "SUCCESS");
        return new AuthResponse(mapUser(user), mapToken(tokenPair));
    }

    /**
     * 濠电偠鎻紞鈧繛澶嬫礋瀵偊濡舵径濠勵槴闂備礁鐏濋鍡涙偂閳ь剚绻涚€电袨闁稿﹥顨嗛弲璺衡攽鐎ｎ偄娈ч悗鐟板濠㈡寰婇崸妤佺厸闁稿本绋戦ˉ瀣箾閸喎鐏︾紒鍌涘浮婵″爼宕惰閺夋椽鏌ｆ惔銊︽锭缂佺姴绉归崺鈧?
     * <p>
     * 闂備礁鎼粙鍕崲濠靛鍋樻繛鍡樻尭缁€鍡涙⒑閸噮鍎愰柣鎾亾濠电偛顕慨浼村磹濡や焦娅犳繝闈涚墢鐏忕敻鎮归崶顏勭毢闁逞屽墴閺€杈╃矙婢跺鍎熼柨婵嗘－娴犻亶姊洪崨濠呭缂佸娼欓悾鐑芥偄閻撳骸鐝樻繝銏ｆ硾椤戝棗鈻嶈箛娑欑厵妞ゆ埈鍓欑粔鍓佺矆婢跺瞼纾兼繛鎴炵懃閸斻倗鎲搁弶鍨偓鍨嚕椤掍緤绱ｅù锝囨嚀琚橀梻浣虹帛椤ㄥ懘鎳熼鐐茬畺婵°倕鎳庣粻濠氭煏閸繍妲归柡浣癸耿閺岋繝宕橀崟顐㈡殭闁绘挻娲熼弻锟犲磼閻戝棙娈ラ梺纭呯堪閸婃繈骞冭娴狅箓宕愰悢椋庡讲闂佽瀛╃粙鎺楁晪闂佺顑呯粔鐟邦嚕椤掍緤绱ｅù锝囨嚀琚橀梻浣虹帛椤ㄥ懐鎮锕€鐒?
     *
     * @param request 闂備礁鎲＄敮锟犲绩闁秴钃熷┑鐘插鐎氭岸姊洪崹顕呭剳婵犫偓閹绢喗鐓ユ繛鎴烆焾鐎氫即鎮楅崹顐€跨€规洏鍎甸、鏇㈩敆閸屾粎宕秗efreshToken闂?
     * @return 闂備礁鎼崐瑙勭珶閸℃せ鏋旈柟瀵稿仧椤╃兘鏌曢崼婵囨悙妞ゃ倕鍊块弻娑樷攽閸℃銏㈢磼閵婏妇鍙€濠?
     * @throws BusinessException 闁荤喐绮庢晶妤呭箰閹间礁鏄ユ繛鎴欏灩濡ê螞妫颁胶鍔嶅┑顔哄灲閺岋絽螖閳ь剟寮甸鈧灋闁靛牆顦弸渚€鏌涢銈呮瀻闁诲繑顨婇弻锝夛綖椤掆偓婵′粙鏌涢埡鍌溾姇缂佸顦濂稿川椤栨稒鍠栭梻渚€娼荤拹鐔煎礉瀹ュ拋娓婚柛宀€鍋涚粻顔界箾閹寸伝顏勑掓径鎰厪?
     */
    public TokenResponse refresh(TokenRefreshRequest request){
        Jwt jwt=decodeRefreshToken(request.refreshToken());

        if(!Objects.equals("refresh",jwtService.extractTokenType(jwt))){
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        long userId=jwtService.extractUserId(jwt);
        String tokenId=jwtService.extractTokenId(jwt);

        if(!refreshTokenStore.isTokenValid(userId,tokenId)){
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        AppUser user=findUserById(userId).orElseThrow(()->new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        TokenPair tokenPair=jwtService.issueTokenPair(user);
        refreshTokenStore.revokeToken(userId,tokenId);
        storeRefreshToken(userId,tokenPair);

        return mapToken(tokenPair);
    }

    /**
     * 濠电偠鎻紞鈧繛澶嬫礋瀵偊濡堕崱鈺傤啍閻庡厜鍋撻柛鎰典簼椤秹姊哄ú璇叉灆缂佽埖宀稿畷鎶藉传閵壯咃紴濡炪倖妫侀崑鎰ｉ弴銏＄厽妞ゆ棁宕甸ˇ锕傛煠閼姐倕鏋戦柟宄版嚇瀹曞綊顢欓懖鈺佸箞闂備礁鎼崐鍧椼€傞敃鍌氱闁靛牆顦伴崑瀣倶閻愬樊娼熷ù婊冦偢閺屸剝鎷呴崫銉ヮ暥闂?
     *
     * @param request 闂傚倷鐒﹁ぐ鍐矓閸洘鍋柛鈩冪懄鐎氭岸姊洪崹顕呭剳婵犫偓閹绢喗鐓ユ繛鎴烆焾鐎氫即鎮楅崹顐€跨€规洏鍎甸、鏇㈩敆閸屾粎宕堕梻浣告惈缁夋潙煤閳哄懏鍎婇柟瀵稿Х鐏忕敻鎮归崶顏勭毢闁逞屽墴閺€杈╃矙婢跺鍎熼柍閿亾闁稿鎸剧划顓犳偘閳ュ厖澹曟繛鎾村焹閸嬫捇鏌ｅ☉娆戞噮闁归濮甸幏鍛存惞閻熸壆浠梻渚€娼уΛ鏃堟倿閿曞倸钃熷┑鐘插閸嬫捇妫冨☉娆樻￥闂佺粯鐗楃划鎾翠繆?
     * @throws BusinessException 闁荤喐绮庢晶妤呭箰婵犳碍鍋夐柨婵嗘处鐎氭岸鏌涢埄鍐╃闁荤喐绻堥幃妤€鈽夊▍顓т簻閿曘垽顢旈崼婢儱銆掑锝呬壕闂佺粯绋掗悷褔骞忛悩铏闁惧繒鎳撻悘锝嗙節閵忊€冲姸缂侇喖澧介幉鎾晝閸屾氨顓洪梺鍝勮癁閸涱噮娼ラ梻浣规た閸犳洟鎳楅崼鏇熷仱闁靛鏅滈崑鈺呮煏婢跺牆鈧鎮峰┑鍥╃瘈濠电偞鍔戦崐鏇㈠窗閺囥垺鐓涢柛鎰靛幖閻撴劕鈹戦鑲╃鐎规洘鐟╅獮渚€骞掗崱妞惧?
     */
    public void resetPassword(PasswordResetRequest request){
        validateIdentifier(request.identifierType(), request.identifier());
        validatePassword(request.newPassword());
        String identifier = normalizeIdentifier(request.identifierType(), request.identifier());
        AppUser user=findUserByIdentifier(request.identifierType(), request.identifier())
                .orElseThrow(()->new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        //闂備礁鎼粙鍕崲濠靛鍋樻繛鍡樺姦濞堢晫鈧厜鍋撻柛鎰典簼椤秹姊哄ú璇叉灆闁绘帪绠撻幃褏绮欏▎鎯ф?
        ensureVerificationSuccess(verificationService.verify(VerificationScene.RESET_PASSWORD,identifier,request.code()));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        userService.updatePassword(user);
        refreshTokenStore.revokeAll(user.getId());
    }
    /**
     * 闂備礁鎼€氼喗鎱ㄩ幘顔藉剭闁绘ê纾々鐑芥煏閸繃鎼愭い銈呭€块幃妤呮偨閸偄娈岄梺绯曟櫅閻倸鐣烽銏╁悑闁告侗鍨伴悾銊╂煟鎼达絾鏆╂い銏狅躬閹稿懘宕熼姣?
     *
     * @param tokenPair 濠电偛顕慨浼村磹濡や焦娅犳繝闈涚墐閸嬫捇妫冨☉姘辨殯闂?
     * @return 濠电偛顕慨浼村磹濡や焦娅犳繝闈涱儏娴肩姷鈧箍鍎遍幊鎰偓鐟邦樀閺?
     */
    private TokenResponse mapToken(TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.accessTokenExpiresAt(), tokenPair.refreshToken(), tokenPair.refreshTokenExpiresAt());
    }

    /**
     * 闂佽瀛╃粙鎺楁晪闂佺顑呯粔鐟扮暦濮樿埖鐓ラ悗锝庡亞閸樻劖绻涚€电袨闁稿﹥顨嗛弲璺衡攽鐎ｎ偄鍓梺鑺ッˇ浼村磹閹惰姤鐓曟俊銈勭劍缁€澶嬨亜閺傛寧绌块柕鍥ㄥ姍瀵潙螣瀹勯澹?
     *
     * @param userId    闂備焦妞垮鍧楀礉瀹ュ鏄?ID闂?
     * @param tokenPair 濠电偛顕慨浼村磹濡や焦娅犳繝闈涚墐閸嬫捇鎮介崹顐户缂備浇椴搁悷鈺佺暦閵夆晩鏁冮柕蹇曞Х閸橀亶姊洪崫鍕偓鍧椼€傞敃鍌氱闁靛牆顦伴崑?ID 濠电偞鍨堕幐绋款潩閿旂偓宕查柛鎰靛枛鐎氬鏌ｉ弬鎸庡暈妞は佸洦鈷掗柛灞剧箖瑜把呯磼鏉堛劌鍝哄┑?
     */
    private void storeRefreshToken(Long userId, TokenPair tokenPair) {
        Duration ttl=Duration.between(Instant.now(),tokenPair.refreshTokenExpiresAt());
        if (ttl.isNegative()){//婵犵妲呴崑鈧柛瀣尰缁绘盯寮堕幋顓涘亾閳х憙l闂備礁鎼€氱兘宕规导鏉戠畾濞达絽婀遍埢鏂课旈敃浣圭《妞わ缚鍗抽弻?
            ttl=Duration.ZERO;
        }

        refreshTokenStore.storeToken(userId,tokenPair.refreshTokenId(),ttl);
    }

    /**
     * 闂備礁鎼€氼喗鎱ㄩ幘顔藉剭闁绘绮崑銊╂煏婵犲繒鐣遍柣鎾存礋閹宕烽鐐扮钵缂傚倸绉撮澶婄暦濮樿泛绾ч柟绋垮閸庛儵鏌熼悡搴ｆ憼闁哄被鍔戦、姘额敊閻愵剙顕￠柟鐑樺▕閸┾偓?
     *
     * @param user 闂備焦妞垮鍧楀礉瀹ュ鏄ユ繛鎴炲焹閸嬫捇宕烽鐐扮钵缂傚倸绉撮澶嬩繆?
     * @return 闂備焦妞垮鍧楀礉瀹ュ鏄ユ繛鎴欏灩娴肩姷鈧箍鍎遍幊鎰偓鐟邦樀閺?
     */
    private AuthUserResponse mapUser(AppUser user){
        return new AuthUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhone(),
                user.getBio(),
                user.getTagsJson()
        );
    }

    /**
     * 闂備焦鐪归崹濠氬窗閹版澘鍨傛慨妯垮煐閸嬨劑鏌曟繝蹇曠暠闁绘挻娲熼弻?
     * @return
     */
    private String generateNickname() {
        return "SpaceMate" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 闂備礁鎼粙鍕崲濠靛鍋樻繛鍡樺灍閸嬫捇妫冨☉娆樻￥闂佺粯鐗楃划搴ｅ垝閻㈢鍐€妞ゆ劧绲鹃娲⒑閹稿海鈯曠€光偓缁嬫鐒芥い蹇撴閻岸鏌熼鐔峰潑闁稿鎸婚幏鍛喆閸曨亞甯涢梺璇茬箰缁绘劗鎹㈠鈧铏綇閵婏箑鍔呴梺鎸庢閵嗏偓闁稿鎸婚幏鍛村传閵壯呮殸濠碉紕鍋戦崐娑㈩敋瑜忛埀顒€鐏氶敃銏犵暦閵夆晩鏁冮柕蹇嬪灪閹茶鈹戦鍡欏埌缂佸鎸抽獮蹇斿閺夋垶鐎梺鍝勵槼濞夋洟骞楅悢鍏肩厪?
     * @param password
     */
    private void validatePassword(String password) {
        if(!StringUtils.hasText(password)){
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password is required");
        }
        String trimmed=password;

        if(trimmed.length()<authProperties.getPassword().getMinLength()){
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password is too short");
        }
        boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);//闂備胶鍘ч崲鏌ュ疮閸ф鍎嶆い鏍仜缁€宀勬煕濞戝崬骞楅柛搴㈡尭閳藉骞橀姘濠电偞鍨堕幖鈺傜濠婂牊鍋ょ憸宥堫暰?
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);//闂備胶鍘ч崲鏌ュ疮閸ф鍎嶆い鏍仜鐎氬顭跨捄渚Ш妞ゅ繐宕埥澶愬箼閸愌呭嚬闂佸搫妫涢崰鎾诲焵?

        if(!hasLetter || !hasDigit){
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password must include letters and digits");
        }
    }

    /**
     * 濠德板€楁慨鎾儗娓氣偓閹焦寰勯幇顓炲壓婵炶揪绲藉﹢杈ㄦ櫠閳ユ緞?
     * @param result
     */
    private void ensureVerificationSuccess(VerificationCheckResult result) {
        if (result.isSuccess()) {
            return;
        }
        VerificationCodeStatus status = result.status();
        if (status == VerificationCodeStatus.NOT_FOUND || status == VerificationCodeStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
        if (status == VerificationCodeStatus.MISMATCH) {
            throw new BusinessException(ErrorCode.VERIFICATION_MISMATCH);
        }
        if (status == VerificationCodeStatus.TOO_MANY_ATTEMPTS) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY_ATTEMPTS);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "濠德板€楁慨鎾儗娓氣偓閹焦寰勯幇顓炲壓婵炶揪绲藉﹢杈ㄦ櫠閳ユ緞鏃堟偐閼艰泛顏梺闈涙处閸ㄥ綊骞?");
    }

    /**
     * 闂備礁鎲＄敮鍥磹閺嶎厼钃熼柛銉墮閸愨偓闂佹悶鍎烘禍鐐参ｉ弴銏＄厸闁告洟娼ч悘鐘绘煙椤栨瑧顦︽い鏂跨箻楠炴捇骞掗弮鈧幉濂告⒑闂堚晞绀嬮柛鏂匡躬閸┾偓?
     *
     * @param identifierType       闂備礁鎼粔鏉懨洪埡鍛剨闁瑰濮风亸鐢告偣閸ヮ亜鐨洪柍褜鍓涢崗姗€寮鍜佹桨闁告牜鍏奛E 闂?EMAIL闂?
     * @param identifier 闂備礁鎼粔鏉懨洪埡鍛剨闁硅揪绠戠壕?闂備焦妞垮鍧楀礉瀹ュ拋鐒介柣銏㈩焾缁€鍡涙煏閸繃顥為柣鎺嶇矙閺岋紕浠﹂幆褍顫堥梻浣稿级缁捇鐛鍥ｅ亾閿濆骸浜濈紒銊﹀哺閺屾稑鈻庨幘瀛樻殸闂佹悶鍔婇崝鎴﹀箠閹捐宸濋柡澶嬪灣缁紕绱撴担鍝勑瀵割焾鍗遍柟闂寸鐟欙箓鏌ㄩ弴妤€浜鹃梺鍛婂煀缁辨洜妲愰幒鏇犵杸婵ê鍚嬮拺澶嬬箾鏉堝墽绉繛澶嬫礋瀵偊濡堕崱娆樻锤闂佺硶妲呴崢鍓х矆閸曨垱鈷掗柛銉到娴滅偓绻涢幋鐐村暗闁稿簺鍊濋幃銉╂晲婢跺﹦顦╅梺绯曞墲椤ㄥ懘顢氳閺屸€愁吋閸パ冧粯缂備焦鍞荤换婵嬪极瀹ュ棗绶為柛顐ｈ壘娴?
     * @return 闂備礁鎼€氱兘宕规导鏉戠畾濞达綀銆€閸嬫挸鈽夊▍顓т簻閿曘垽顢旈崼婢?
     */
    private boolean identifierExists(@NotNull(message = "Identifier type is required") IdentifierType identifierType, @NotBlank(message = "Identifier is required") String identifier) {
        return switch (identifierType) {
            case PHONE -> userService.existsByPhone(identifier);
            case EMAIL -> userService.existsByEmail(identifier);
        };
    }



    private <T> Optional<AppUser> findUserById(long userId) {
        return userService.findById(userId);
    }


    /**
     * 闂備礁鎼粔鏉懨洪妶澶婇棷妞ゆ牜鍋涚粈宀勬煛瀹ュ啫濡介柣搴櫍閺?
     * @param identifierType
     * @param identifier
     * @return
     */
    private String normalizeIdentifier(IdentifierType identifierType, String identifier) {
        return switch (identifierType) {
            case EMAIL -> identifier.trim();
            case PHONE -> identifier.trim().toLowerCase();
        };
    }

    /**
     * 闂備礁鎼粙鍕崲濠靛鍋樻繛鍡樻尭缁犮儵鎮楅敐搴′簼缂併劍宀搁弻娑樷枎閹惧瓨鏆犻梺鎼炲妸閸旀垿寮婚崶顒夋晩缂備降鍨归弫濠氭⒑閸濆嫮肖闁告鍛潟?
     * @param identifierType
     * @param identifier
     */
    private void validateIdentifier( IdentifierType identifierType,  String identifier) {
        if(identifierType== PHONE&&!IdentifierValidator.isValidPhone(identifier)){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "闂備礁缍婂褔顢栭崱妞绘敠闁逞屽墴閺屾稑鈻庨幘瀛樻殸闂佺粯顨堥…鍫ヮ敋閿濆牏鐤€婵炴垶鐟﹀▍鎾绘煟?");
        }

        if(identifierType== EMAIL&&!IdentifierValidator.isValidEmail(identifier)){
            throw new BusinessException(ErrorCode.BAD_REQUEST, "闂傚倷绶￠崣搴ㄥ窗濮橀鏁婇柛顐犲劚閸愨偓闂佺粯鎸稿ù椋庢兜閳ь剟姊绘担鐟邦嚋闁荤喆鍔戦、?");
        }
    }



    /**
     * 闂佽崵鍠愰悷杈╁緤閸ф鍋夋繝濠傜墕缁€鍡涙⒑閸噮鍎愰柣鎾亾濠电偛顕慨浼村磹濡や焦娅犳繝闈涱儐閺咁剟鎮橀悙宸綗濞存粌銈搁幃褰掑箛閳轰礁濮曞┑锛勫仜閸婂潡鐛惔鈾€妲堟俊銈呭暟閹虫繈姊洪崨濠冪厸闁逞屽墮绾绢厾澹曟禒瀣仺妞ゆ牗绋掗崑鐘绘煃?
     *
     * @param refreshToken 闂備礁鎲＄敮锟犲绩闁秴钃熷┑鐘插暟椤╃兘鏌曢崼婵囨悙妞ゃ倕鍊块幃妤€鈽夊▎妯荤暭濡炪倖鍨抽悞锔剧矙婢跺奔娌柛灞绢殔娴?
     * @return 闂佽崵鍠愰悷杈╁緤妤ｅ啯鍊甸柦妯侯樈閸ゆ淇婇妶鍌氫壕闂佺硶鏅涢惌鍌炲箚?JWT闂?
     * @throws BusinessException 闁荤喐绮庢晶妤呭箰閹间礁鏄ユ繛鎴欏灩濡ê螞妫颁胶鍔嶅┑顔哄灲閺岋絽螖閳ь剟寮甸鈧灋闁靛牆鎳夐弸鏍煛閸ワ絽顕￠柟鐑橆殔閸戠娀鏌熼柇锕€骞楁い蟻鍥ㄧ厵闁煎摜鏁搁埥澶愭煕閵堝骸寮┑?
     */
    private Optional<Jwt> decodeRefreshTokenSafely(String refreshToken) {
        try{
            return Optional.of(jwtService.decode(refreshToken));
        }catch (JwtException e){
            return Optional.empty();
        }
    }

    private Jwt decodeRefreshToken(@NotBlank(message = "Refresh token is required") String refreshToken) {
        try{
            return jwtService.decode(refreshToken);
        }catch (JwtException ex){
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }
    private Optional<AppUser> findUserByIdentifier(@NotNull(message = "Identifier type is required") IdentifierType identifierType, @NotBlank(message = "Identifier is required") String identifier) {
        return switch (identifierType){
            case PHONE -> userService.findByPhone(identifier);
            case EMAIL ->  userService.findByEmail(identifier);
        };
    }
}


