package com.spacemate.modules.admin.service;

import com.spacemate.modules.admin.dto.response.AdminDashboardOverviewResponse;
import java.time.LocalDate;

public interface AdminDashboardService {

    AdminDashboardOverviewResponse overview(LocalDate date);
}



