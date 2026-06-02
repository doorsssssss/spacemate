package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spacemate.modules.admin.dto.response.AdminDashboardOverviewResponse;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.spacemate.modules.admin.service.AdminBookingService;
import com.spacemate.modules.admin.service.AdminDashboardService;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminBookingService adminBookingService;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;

    public AdminDashboardServiceImpl(AdminBookingService adminBookingService, SeatMapper seatMapper, BookingMapper bookingMapper) {
        this.adminBookingService = adminBookingService;
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
    }

    public AdminDashboardOverviewResponse overview(LocalDate date) {
        long total = adminBookingService.countTotalForDate(date);
        long pending = adminBookingService.countByStatusForDate(date, 1);
        long used = adminBookingService.countByStatusForDate(date, 2);
        long cancelled = adminBookingService.countByStatusForDate(date, 3);

        long activeSeatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getStatus, 1));
        List<Booking> activeBookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
            .in(Booking::getStatus, 1, 2)
            .ge(Booking::getStartAt, date.atStartOfDay())
            .lt(Booking::getStartAt, date.plusDays(1).atStartOfDay())
            .select(Booking::getSeatId));
        long usedSeatCount = activeBookings.stream()
            .map(Booking::getSeatId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet())
            .size();
        double occupancyRate = activeSeatCount == 0 ? 0.0 : (double) usedSeatCount / activeSeatCount;

        AdminDashboardOverviewResponse response = new AdminDashboardOverviewResponse();
        response.setDate(date.toString());
        response.setTotalBookings(total);
        response.setPendingBookings(pending);
        response.setUsedBookings(used);
        response.setCancelledBookings(cancelled);
        response.setOccupancyRate(occupancyRate);
        return response;
    }
}



