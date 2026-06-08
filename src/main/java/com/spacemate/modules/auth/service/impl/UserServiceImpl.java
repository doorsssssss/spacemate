package com.spacemate.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.infrastructure.persistence.mapper.UserMapper;
import com.spacemate.modules.auth.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * 根据手机号查询用户。
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByPhone(String phone) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getPhone, phone);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper));
    }

    /**
     * 根据邮箱查询用户。
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getEmail, email);
        return Optional.ofNullable(userMapper.selectOne(queryWrapper));
    }

    /**
     * 根据 ID 查询用户。
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findById(long id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    /**
     * 判断手机号是否已存在。
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getPhone, phone);
        return userMapper.exists(queryWrapper);
    }

    /**
     * 判断邮箱是否已存在。
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUser::getEmail, email);
        return userMapper.exists(wrapper);
    }

    /**
     * 创建用户并维护创建/更新时间。
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
     * 更新用户密码。
     */
    @Transactional
    @Override
    public void updatePassword(AppUser user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
