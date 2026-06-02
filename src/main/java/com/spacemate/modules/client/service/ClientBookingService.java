package com.spacemate.modules.client.service;

import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.request.ClientCreateBookingRequest;
import com.spacemate.modules.client.dto.response.ClientBookingResponse;
import java.time.LocalDate;

public interface ClientBookingService {

    ClientBookingResponse create(String phone, ClientCreateBookingRequest request);

    PageResponse<ClientBookingResponse> mine(
        String phone,
        Integer status,
        LocalDate startDate,
        LocalDate endDate,
        long page,
        long size
    );

    ClientBookingResponse detail(String phone, Long bookingId);

    ClientBookingResponse cancel(String phone, Long bookingId, String cancelReason);
}



