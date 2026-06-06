package com.spacemate.modules.client.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.modules.client.dto.request.ClientCreateCommentRequest;
import com.spacemate.modules.client.dto.response.ClientCommentLikeResponse;
import com.spacemate.modules.client.dto.response.ClientCommentResponse;
import com.spacemate.modules.client.service.ClientCommentService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
public class ClientCommentController {

    private final ClientCommentService clientCommentService;

    public ClientCommentController(ClientCommentService clientCommentService) {
        this.clientCommentService = clientCommentService;
    }

    @GetMapping("/spaces/{spaceId}/comments")
    public ApiResponse<PageResponse<ClientCommentResponse>> list(
        @PathVariable Long spaceId,
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(clientCommentService.list(spaceId, resolveOptionalPhone(userPhone, phoneQuery), page, size));
    }

    @PostMapping("/spaces/{spaceId}/comments")
    public ApiResponse<ClientCommentResponse> create(
        @PathVariable Long spaceId,
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery,
        @Valid @RequestBody ClientCreateCommentRequest request
    ) {
        return ApiResponse.ok(clientCommentService.create(spaceId, resolveRequiredPhone(userPhone, phoneQuery), request));
    }

    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<ClientCommentLikeResponse> like(
        @PathVariable Long commentId,
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery
    ) {
        return ApiResponse.ok(clientCommentService.like(commentId, resolveRequiredPhone(userPhone, phoneQuery)));
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ApiResponse<ClientCommentLikeResponse> unlike(
        @PathVariable Long commentId,
        @RequestHeader(value = "X-User-Phone", required = false) String userPhone,
        @RequestParam(value = "phone", required = false) String phoneQuery
    ) {
        return ApiResponse.ok(clientCommentService.unlike(commentId, resolveRequiredPhone(userPhone, phoneQuery)));
    }

    private String resolveRequiredPhone(String headerPhone, String queryPhone) {
        String phone = resolveOptionalPhone(headerPhone, queryPhone);
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(401, "请先登录或提供手机号");
        }
        return phone;
    }

    private String resolveOptionalPhone(String headerPhone, String queryPhone) {
        return StringUtils.hasText(headerPhone) ? headerPhone : queryPhone;
    }
}