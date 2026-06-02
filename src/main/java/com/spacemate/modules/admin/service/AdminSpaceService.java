package com.spacemate.modules.admin.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSpaceRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSpaceRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminSpaceResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.domain.entity.Space;

public interface AdminSpaceService {

    PageResponse<AdminSpaceResponse> list(String keyword, Integer status, long page, long size);

    AdminSimpleIdResponse create(AdminCreateSpaceRequest request);

    AdminUpdateResultResponse update(Long id, AdminUpdateSpaceRequest request);

    AdminDeleteResultResponse delete(Long id);

    AdminSpaceResponse detail(Long id);

    Space requireById(Long id);
}



