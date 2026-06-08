package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.util.BeanMergeUtils;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.infrastructure.persistence.mapper.AppUserMapper;
import com.spacemate.modules.admin.dto.request.AdminCreateUserRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateUserRequest;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUserResponse;
import com.spacemate.modules.admin.service.AdminUserService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AppUserMapper appUserMapper;

    public AdminUserServiceImpl(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public PageResponse<AdminUserResponse> list(String phone, Integer status, long page, long size) {
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(phone)) {
            wrapper.like(AppUser::getPhone, phone);
        }
        if (status != null) {
            wrapper.eq(AppUser::getStatus, status);
        }
        wrapper.orderByDesc(AppUser::getId);
        Page<AppUser> pageResult = appUserMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserResponse> items = pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    public AdminUserResponse detail(Long id) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toResponse(user);
    }

    @Transactional
    public AdminUpdateResultResponse create(AdminCreateUserRequest request) {
        AppUser exists = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, request.getPhone()));
        if (exists != null) {
            throw new BusinessException(409, "手机号已存在");
        }
        AppUser user = new AppUser();
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        appUserMapper.insert(user);
        return new AdminUpdateResultResponse(user.getId(), true);
    }

    @Transactional
    public AdminUpdateResultResponse update(Long id, AdminUpdateUserRequest request) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (request.getPhone() != null) {
            AppUser duplicate = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, request.getPhone())
                .ne(AppUser::getId, id));
            if (duplicate != null) {
                throw new BusinessException(409, "手机号已存在");
            }
        }
        BeanUtils.copyProperties(request, user, BeanMergeUtils.nullPropertyNames(request));
        appUserMapper.updateById(user);
        return new AdminUpdateResultResponse(id, true);
    }

    @Transactional
    public AdminUpdateResultResponse updateStatus(Long id, Integer status) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 9 && status == 0) {
            throw new BusinessException(409, "不能禁用管理员账号");
        }
        user.setStatus(status);
        int rows = appUserMapper.updateById(user);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    @Transactional
    public AdminUpdateResultResponse delete(Long id) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getRole() != null && user.getRole() == 9) {
            throw new BusinessException(409, "不能删除管理员账号");
        }
        user.setDeleted(1);
        user.setStatus(0);
        user.setDeletedAt(java.time.LocalDateTime.now());
        appUserMapper.updateById(user);
        return new AdminUpdateResultResponse(id, true);
    }

    private AdminUserResponse toResponse(AppUser user) {
        AdminUserResponse response = new AdminUserResponse();
        BeanUtils.copyProperties(user, response);
        response.setPhone(maskPhone(user.getPhone()));
        return response;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
