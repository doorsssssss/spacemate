package com.spacemate.modules.admin.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminUpdateBookingStatusRequest;
import com.spacemate.modules.admin.dto.response.AdminBookingResponse;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminBookingService;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminBookingResponse>> list(
        @RequestParam(required = false) Long spaceId,
        @RequestParam(required = false) Long seatId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(adminBookingService.list(spaceId, seatId, phone, status, startAt, endAt, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminBookingResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminBookingService.detail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AdminUpdateResultResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdminUpdateBookingStatusRequest request
    ) {
        return ApiResponse.ok(adminBookingService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<AdminDeleteResultResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(adminBookingService.delete(id));
    }
}



