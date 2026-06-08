package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.AppUserMapper;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import com.spacemate.modules.admin.dto.request.AdminUpdateBookingStatusRequest;
import com.spacemate.modules.admin.dto.response.AdminBookingResponse;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminBookingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingMapper bookingMapper;
    private final AppUserMapper appUserMapper;
    private final SpaceMapper spaceMapper;
    private final SeatMapper seatMapper;

    public AdminBookingServiceImpl(BookingMapper bookingMapper, AppUserMapper appUserMapper, SpaceMapper spaceMapper, SeatMapper seatMapper) {
        this.bookingMapper = bookingMapper;
        this.appUserMapper = appUserMapper;
        this.spaceMapper = spaceMapper;
        this.seatMapper = seatMapper;
    }

    public PageResponse<AdminBookingResponse> list(
        Long spaceId,
        Long seatId,
        String phone,
        Integer status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long page,
        long size
    ) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        if (spaceId != null) {
            wrapper.eq(Booking::getSpaceId, spaceId);
        }
        if (seatId != null) {
            wrapper.eq(Booking::getSeatId, seatId);
        }
        if (status != null) {
            wrapper.eq(Booking::getStatus, status);
        }
        if (startAt != null) {
            wrapper.ge(Booking::getStartAt, startAt);
        }
        if (endAt != null) {
            wrapper.le(Booking::getEndAt, endAt);
        }
        if (StringUtils.hasText(phone)) {
            AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
            if (user == null) {
                return new PageResponse<>(java.util.Collections.emptyList(), page, size, 0);
            }
            wrapper.eq(Booking::getUserId, user.getId());
        }
        wrapper.orderByDesc(Booking::getId);

        Page<Booking> pageResult = bookingMapper.selectPage(new Page<>(page, size), wrapper);
        List<Booking> bookings = pageResult.getRecords();
        Map<Long, AppUser> userMap = buildUserMap(bookings);
        Map<Long, Space> spaceMap = buildSpaceMap(bookings);
        Map<Long, Seat> seatMap = buildSeatMap(bookings);

        List<AdminBookingResponse> items = bookings.stream().map(item -> toResponse(item, userMap, spaceMap, seatMap)).collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    @Transactional
    public AdminUpdateResultResponse updateStatus(Long id, AdminUpdateBookingStatusRequest request) {
        Booking booking = requireById(id);
        if (booking.getStatus() != 1 && request.getStatus() == 2) {
            throw new BusinessException(409, "仅待使用预约可以标记为已使用");
        }
        if (booking.getStatus() == 3) {
            throw new BusinessException(409, "已取消预约不能变更状态");
        }
        booking.setStatus(request.getStatus());
        if (request.getStatus() == 3) {
            booking.setCancelReason(request.getReason());
        }
        int rows = bookingMapper.updateById(booking);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    @Transactional
    public AdminDeleteResultResponse delete(Long id) {
        Booking booking = requireById(id);
        booking.setDeleted(1);
        booking.setStatus(3);
        booking.setDeletedAt(LocalDateTime.now());
        if (!StringUtils.hasText(booking.getCancelReason())) {
            booking.setCancelReason("管理员删除");
        }
        int rows = bookingMapper.updateById(booking);
        return new AdminDeleteResultResponse(id, rows > 0);
    }

    public AdminBookingResponse detail(Long id) {
        Booking booking = requireById(id);
        Map<Long, AppUser> userMap = buildUserMap(java.util.Collections.singletonList(booking));
        Map<Long, Space> spaceMap = buildSpaceMap(java.util.Collections.singletonList(booking));
        Map<Long, Seat> seatMap = buildSeatMap(java.util.Collections.singletonList(booking));
        return toResponse(booking, userMap, spaceMap, seatMap);
    }

    public Booking requireById(Long id) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException(404, "预约不存在");
        }
        return booking;
    }

    public long countTotalForDate(LocalDate date) {
        return bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
            .ge(Booking::getStartAt, date.atStartOfDay())
            .lt(Booking::getStartAt, date.plusDays(1).atStartOfDay()));
    }

    public long countByStatusForDate(LocalDate date, int status) {
        return bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
            .eq(Booking::getStatus, status)
            .ge(Booking::getStartAt, date.atStartOfDay())
            .lt(Booking::getStartAt, date.plusDays(1).atStartOfDay()));
    }

    private Map<Long, AppUser> buildUserMap(List<Booking> bookings) {
        Set<Long> userIds = bookings.stream().map(Booking::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<AppUser> users = appUserMapper.selectList(new LambdaQueryWrapper<AppUser>().in(AppUser::getId, userIds));
        return users.stream().collect(Collectors.toMap(AppUser::getId, u -> u));
    }

    private Map<Long, Space> buildSpaceMap(List<Booking> bookings) {
        Set<Long> ids = bookings.stream().map(Booking::getSpaceId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        List<Space> spaces = spaceMapper.selectList(new LambdaQueryWrapper<Space>().in(Space::getId, ids));
        return spaces.stream().collect(Collectors.toMap(Space::getId, s -> s));
    }

    private Map<Long, Seat> buildSeatMap(List<Booking> bookings) {
        Set<Long> ids = bookings.stream().map(Booking::getSeatId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>().in(Seat::getId, ids));
        return seats.stream().collect(Collectors.toMap(Seat::getId, s -> s));
    }

    private AdminBookingResponse toResponse(Booking booking, Map<Long, AppUser> userMap, Map<Long, Space> spaceMap, Map<Long, Seat> seatMap) {
        AdminBookingResponse response = new AdminBookingResponse();
        BeanUtils.copyProperties(booking, response);
        AppUser user = userMap.get(booking.getUserId());
        if (user != null) {
            response.setPhone(maskPhone(user.getPhone()));
        }
        Space space = spaceMap.get(booking.getSpaceId());
        if (space != null) {
            response.setSpaceName(space.getName());
        }
        Seat seat = seatMap.get(booking.getSeatId());
        if (seat != null) {
            response.setSeatNumber(seat.getSeatNumber());
        }
        return response;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
