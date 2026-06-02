package com.spacemate.modules.admin.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateUserRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateUserStatusRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateUserRequest;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUserResponse;
import com.spacemate.modules.admin.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> list(
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(adminUserService.list(phone, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.detail(id));
    }

    @PostMapping
    public ApiResponse<AdminUpdateResultResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(adminUserService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUpdateResultResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.ok(adminUserService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminUpdateResultResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdminUpdateUserStatusRequest request
    ) {
        return ApiResponse.ok(adminUserService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AdminUpdateResultResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.delete(id));
    }
}



