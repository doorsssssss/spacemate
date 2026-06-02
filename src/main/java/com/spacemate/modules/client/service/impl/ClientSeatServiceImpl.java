package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.response.ClientSeatResponse;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.spacemate.modules.client.service.ClientSeatService;
import com.spacemate.modules.client.service.ClientSpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class ClientSeatServiceImpl implements ClientSeatService {

    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final ClientSpaceService clientSpaceService;

    public ClientSeatServiceImpl(SeatMapper seatMapper, BookingMapper bookingMapper, ClientSpaceService clientSpaceService) {
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.clientSpaceService = clientSpaceService;
    }

    public PageResponse<ClientSeatResponse> available(
        Long spaceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer hasSocket,
        Integer isQuiet,
        long page,
        long size
    ) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BusinessException(400, "閺冨爼妫垮▓鍏哥瑝閸氬牊纭?");
        }
        clientSpaceService.requireActiveSpace(spaceId);

        LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, spaceId)
            .eq(Seat::getStatus, 1)
            .orderByAsc(Seat::getSeatNumber);
        if (hasSocket != null) {
            seatWrapper.eq(Seat::getHasSocket, hasSocket);
        }
        if (isQuiet != null) {
            seatWrapper.eq(Seat::getIsQuiet, isQuiet);
        }
        List<Seat> allSeats = seatMapper.selectList(seatWrapper);
        if (allSeats.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), page, size, 0);
        }

        List<Long> seatIds = allSeats.stream().map(Seat::getId).collect(Collectors.toList());
        List<Booking> conflictBookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
            .in(Booking::getSeatId, seatIds)
            .in(Booking::getStatus, 1, 2)
            .lt(Booking::getStartAt, endAt)
            .gt(Booking::getEndAt, startAt)
        );
        Set<Long> conflictSeatIds = conflictBookings.stream().map(Booking::getSeatId).collect(Collectors.toSet());

        List<Seat> availableSeats = allSeats.stream()
            .filter(s -> !conflictSeatIds.contains(s.getId()))
            .collect(Collectors.toList());
        long total = availableSeats.size();

        long from = Math.max(0, (page - 1) * size);
        long to = Math.min(total, from + size);
        if (from >= to) {
            return new PageResponse<>(Collections.emptyList(), page, size, total);
        }
        List<ClientSeatResponse> items = availableSeats.subList((int) from, (int) to).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, total);
    }

    public ClientSeatResponse detail(Long id) {
        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getId, id)
            .eq(Seat::getStatus, 1));
        if (seat == null) {
            throw new BusinessException(404, "鎼囱傜秴娑撳秴鐡ㄩ崷?");
        }
        return toResponse(seat);
    }

    public Seat requireActiveSeat(Long id) {
        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getId, id)
            .eq(Seat::getStatus, 1));
        if (seat == null) {
            throw new BusinessException(404, "鎼囱傜秴娑撳秴鐡ㄩ崷銊﹀灗瀹告彃浠犻悽?");
        }
        return seat;
    }

    private ClientSeatResponse toResponse(Seat seat) {
        ClientSeatResponse response = new ClientSeatResponse();
        BeanUtils.copyProperties(seat, response);
        response.setHasSocket(seat.getHasSocket() != null && seat.getHasSocket() == 1);
        response.setIsQuiet(seat.getIsQuiet() != null && seat.getIsQuiet() == 1);
        return response;
    }
}




