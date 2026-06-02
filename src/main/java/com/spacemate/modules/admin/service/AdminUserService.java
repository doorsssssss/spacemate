package com.spacemate.modules.admin.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateUserRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateUserRequest;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUserResponse;

public interface AdminUserService {

    PageResponse<AdminUserResponse> list(String phone, Integer status, long page, long size);

    AdminUserResponse detail(Long id);

    AdminUpdateResultResponse create(AdminCreateUserRequest request);

    AdminUpdateResultResponse update(Long id, AdminUpdateUserRequest request);

    AdminUpdateResultResponse updateStatus(Long id, Integer status);

    AdminUpdateResultResponse delete(Long id);
}



