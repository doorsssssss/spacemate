package com.spacemate.modules.client.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.modules.client.dto.request.ClientCancelBookingRequest;
import com.spacemate.modules.client.dto.request.ClientCreateBookingRequest;
import com.spacemate.modules.client.dto.response.ClientBookingResponse;
import com.spacemate.modules.client.service.ClientBookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class ClientBookingController {

    private final ClientBookingService clientBookingService;

    public ClientBookingController(ClientBookingService clientBookingService) {
        this.clientBookingService = clientBookingService;
    }

    @PostMapping
    public ApiResponse<ClientBookingResponse> create(
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @Valid @RequestBody ClientCreateBookingRequest request
    ) {
        return ApiResponse.ok(clientBookingService.create(resolvePhone(userPhone, phoneQuery), request));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ClientBookingResponse>> mine(
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(clientBookingService.mine(resolvePhone(userPhone, phoneQuery), status, startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientBookingResponse> detail(
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(clientBookingService.detail(resolvePhone(userPhone, phoneQuery), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ClientBookingResponse> cancel(
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @PathVariable Long id,
        @RequestBody(required = false) ClientCancelBookingRequest request
    ) {
        String reason = request == null ? null : request.getCancelReason();
        return ApiResponse.ok(clientBookingService.cancel(resolvePhone(userPhone, phoneQuery), id, reason));
    }

    private String resolvePhone(String headerPhone, String queryPhone) {
        String phone = StringUtils.hasText(headerPhone) ? headerPhone : queryPhone;
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(401, "请先登录或提供手机号");
        }
        return phone;
    }
}
