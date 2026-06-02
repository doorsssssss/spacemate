package com.spacemate.modules.client.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.response.ClientSeatResponse;
import com.spacemate.modules.client.service.ClientSeatService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seats")
public class ClientSeatController {

    private final ClientSeatService clientSeatService;

    public ClientSeatController(ClientSeatService clientSeatService) {
        this.clientSeatService = clientSeatService;
    }

    @GetMapping("/available")
    public ApiResponse<PageResponse<ClientSeatResponse>> available(
        @RequestParam Long spaceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
        @RequestParam(required = false) Integer hasSocket,
        @RequestParam(required = false) Integer isQuiet,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(clientSeatService.available(spaceId, startAt, endAt, hasSocket, isQuiet, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientSeatResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(clientSeatService.detail(id));
    }
}



