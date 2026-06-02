package com.spacemate.modules.admin.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSeatRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSeatRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSeatResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminSeatService;
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
@RequestMapping("/api/v1/admin/seats")
public class AdminSeatController {

    private final AdminSeatService adminSeatService;

    public AdminSeatController(AdminSeatService adminSeatService) {
        this.adminSeatService = adminSeatService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminSeatResponse>> list(
        @RequestParam(required = false) Long spaceId,
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(adminSeatService.list(spaceId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminSeatResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminSeatService.detail(id));
    }

    @PostMapping
    public ApiResponse<AdminSimpleIdResponse> create(@Valid @RequestBody AdminCreateSeatRequest request) {
        return ApiResponse.ok(adminSeatService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUpdateResultResponse> update(@PathVariable Long id, @Valid @RequestBody AdminUpdateSeatRequest request) {
        return ApiResponse.ok(adminSeatService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AdminDeleteResultResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(adminSeatService.delete(id));
    }
}



