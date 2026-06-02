package com.spacemate.modules.admin.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminUpdateBookingStatusRequest;
import com.spacemate.modules.admin.dto.response.AdminBookingResponse;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.domain.entity.Booking;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AdminBookingService {

    PageResponse<AdminBookingResponse> list(
        Long spaceId,
        Long seatId,
        String phone,
        Integer status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long page,
        long size
    );

    AdminUpdateResultResponse updateStatus(Long id, AdminUpdateBookingStatusRequest request);

    AdminDeleteResultResponse delete(Long id);

    AdminBookingResponse detail(Long id);

    Booking requireById(Long id);

    long countTotalForDate(LocalDate date);

    long countByStatusForDate(LocalDate date, int status);
}



