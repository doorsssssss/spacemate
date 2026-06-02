package com.spacemate.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.infrastructure.persistence.mapper.UserMapper;
import com.spacemate.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    /**
     * 鏍规嵁鎵嬫満鍙锋煡璇㈢敤鎴枫€?
     *
     * @param phone 鎵嬫満鍙枫€?
     * @return 鐢ㄦ埛 Optional銆?
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByPhone(String phone) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getPhone, phone);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper));
    }


    /**
     * 鏍规嵁閭鏌ヨ鐢ㄦ埛銆?
     *
     * @param email 閭鍦板潃銆?
     * @return 鐢ㄦ埛 Optional銆?
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getEmail,email);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper));
    }

    /**
     * 鏍规嵁 ID 鏌ヨ鐢ㄦ埛銆?
     *
     * @param id 鐢ㄦ埛 ID銆?
     * @return 鐢ㄦ埛 Optional銆?
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findById(long id) {
       return Optional.ofNullable(userMapper.selectById(id));
    }

    /**
     * 鍒ゆ柇鎵嬫満鍙锋槸鍚﹀瓨鍦ㄣ€?
     *
     * @param phone 鎵嬫満鍙枫€?
     * @return 鏄惁瀛樺湪銆?
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getPhone, phone);
        return userMapper.exists(queryWrapper);
    }

    /**
     * 鍒ゆ柇閭鏄惁瀛樺湪銆?
     *
     * @param email 閭鍦板潃銆?
     * @return 鏄惁瀛樺湪銆?
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getEmail, email);
        return userMapper.exists(wrapper);
    }

    /**
     * 鍒涘缓鐢ㄦ埛锛屽啓鍏ュ垱寤轰笌鏇存柊鏃堕棿骞舵寔涔呭寲銆?
     *
     * @param user 寰呭垱寤虹殑鐢ㄦ埛瀹炰綋銆?
     * @return 鎸佷箙鍖栧悗鐨勭敤鎴峰疄浣撱€?
     */
    @Transactional
    @Override
    public AppUser createUser(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }

    /**
     * 鏇存柊瀵嗙爜
     * @param user
     */
    @Transactional
    @Override
    public void updatePassword(AppUser user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}


