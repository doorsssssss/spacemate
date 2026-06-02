package com.spacemate.modules.admin.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSeatRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSeatRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSeatResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.domain.entity.Seat;

public interface AdminSeatService {

    PageResponse<AdminSeatResponse> list(Long spaceId, Integer status, long page, long size);

    AdminSimpleIdResponse create(AdminCreateSeatRequest request);

    AdminUpdateResultResponse update(Long id, AdminUpdateSeatRequest request);

    AdminDeleteResultResponse delete(Long id);

    AdminSeatResponse detail(Long id);

    Seat requireById(Long id);
}



