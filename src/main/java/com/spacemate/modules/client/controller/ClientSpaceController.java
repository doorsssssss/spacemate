package com.spacemate.modules.client.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import com.spacemate.modules.client.service.ClientSpaceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/spaces")
public class ClientSpaceController {

    private final ClientSpaceService clientSpaceService;

    public ClientSpaceController(ClientSpaceService clientSpaceService) {
        this.clientSpaceService = clientSpaceService;
    }

    @GetMapping
    public ApiResponse<List<ClientSpaceResponse>> list() {
        return ApiResponse.ok(clientSpaceService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientSpaceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(clientSpaceService.detail(id));
    }
}



