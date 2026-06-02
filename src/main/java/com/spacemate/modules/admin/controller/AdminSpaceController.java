package com.spacemate.modules.admin.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSpaceRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSpaceRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminSpaceResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminSpaceService;
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
@RequestMapping("/api/v1/admin/spaces")
public class AdminSpaceController {

    private final AdminSpaceService adminSpaceService;

    public AdminSpaceController(AdminSpaceService adminSpaceService) {
        this.adminSpaceService = adminSpaceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminSpaceResponse>> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(adminSpaceService.list(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminSpaceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminSpaceService.detail(id));
    }

    @PostMapping
    public ApiResponse<AdminSimpleIdResponse> create(@Valid @RequestBody AdminCreateSpaceRequest request) {
        return ApiResponse.ok(adminSpaceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUpdateResultResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateSpaceRequest request) {
        return ApiResponse.ok(adminSpaceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AdminDeleteResultResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(adminSpaceService.delete(id));
    }
}



