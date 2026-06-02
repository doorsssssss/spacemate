package com.spacemate.modules.admin.controller;

import com.spacemate.common.api.ApiResponse;
import com.spacemate.modules.admin.dto.response.AdminDashboardOverviewResponse;
import com.spacemate.modules.admin.service.AdminDashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminDashboardOverviewResponse> overview(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return ApiResponse.ok(adminDashboardService.overview(targetDate));
    }
}



