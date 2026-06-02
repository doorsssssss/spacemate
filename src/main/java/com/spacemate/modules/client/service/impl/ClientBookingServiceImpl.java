package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.client.dto.request.ClientCreateBookingRequest;
import com.spacemate.modules.client.dto.response.ClientBookingResponse;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.AppUserMapper;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.spacemate.modules.client.service.ClientBookingService;
import com.spacemate.modules.client.service.ClientSeatService;
import com.spacemate.modules.client.service.ClientSpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClientBookingServiceImpl implements ClientBookingService {

    private static final DateTimeFormatter BOOKING_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();

    private final BookingMapper bookingMapper;
    private final AppUserMapper appUserMapper;
    private final ClientSeatService clientSeatService;
    private final ClientSpaceService clientSpaceService;
    private final SeatMapper seatMapper;
    private final SpaceMapper spaceMapper;

    public ClientBookingServiceImpl(
        BookingMapper bookingMapper,
        AppUserMapper appUserMapper,
        ClientSeatService clientSeatService,
        ClientSpaceService clientSpaceService,
        SeatMapper seatMapper,
        SpaceMapper spaceMapper
    ) {
        this.bookingMapper = bookingMapper;
        this.appUserMapper = appUserMapper;
        this.clientSeatService = clientSeatService;
        this.clientSpaceService = clientSpaceService;
        this.seatMapper = seatMapper;
        this.spaceMapper = spaceMapper;
    }

    @Transactional
    public ClientBookingResponse create(String phone, ClientCreateBookingRequest request) {
        validatePhone(phone);
        if (request.getStartAt() == null || request.getEndAt() == null || !request.getStartAt().isBefore(request.getEndAt())) {
            throw new BusinessException(400, "Invalid booking time range");
        }
        if (!request.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "濠电偛顕慨鎾箠韫囨稑鏋佹い鏇楀亾妤犵偞鍔栫粭鐔煎焵椤掑嫨鈧倿鍩￠崘顏咃紡闂佹寧娲嶉崑鎾剁磽瀹ュ拋鍎旂€规洘绮岄濂稿幢濮楀棙袧闂傚倸鍊搁崐鑽ゆ崲濠靛牏涓嶉柨婵嗩槸鐟欙箓骞栨潏鍓хɑ闁伙綁浜堕弻锟犲礃椤撶偟鍘?");
        }

        AppUser user = findOrCreateUser(phone);
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "闂備焦妞垮鍧楀礉瀹ュ鏄ユ繛鎴烇供閸熷懘鏌ゆ總鍓叉澓婵炵厧澧庣槐鎺楀磼濞嗘帒鍘￠梺?");
        }

        Seat seat = clientSeatService.requireActiveSeat(request.getSeatId());
        Space space = clientSpaceService.requireActiveSpace(seat.getSpaceId());
        checkConflict(seat.getId(), request.getStartAt(), request.getEndAt());

        Booking booking = new Booking();
        booking.setBookingNo(generateBookingNo());
        booking.setUserId(user.getId());
        booking.setSpaceId(space.getId());
        booking.setSeatId(seat.getId());
        booking.setStartAt(request.getStartAt());
        booking.setEndAt(request.getEndAt());
        booking.setStatus(1);
        booking.setConfirmCode(generateConfirmCode());
        bookingMapper.insert(booking);

        return toResponse(booking, seat.getSeatNumber(), space.getName());
    }

    public PageResponse<ClientBookingResponse> mine(
        String phone,
        Integer status,
        LocalDate startDate,
        LocalDate endDate,
        long page,
        long size
    ) {
        validatePhone(phone);
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (user == null) {
            return new PageResponse<>(java.util.Collections.emptyList(), page, size, 0);
        }
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<Booking>().eq(Booking::getUserId, user.getId());
        if (status != null) {
            wrapper.eq(Booking::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(Booking::getStartAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(Booking::getStartAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(Booking::getStartAt);
        Page<Booking> pageResult = bookingMapper.selectPage(new Page<>(page, size), wrapper);

        List<Booking> bookings = pageResult.getRecords();
        Map<Long, String> seatNameMap = buildSeatNameMap(bookings);
        Map<Long, String> spaceNameMap = buildSpaceNameMap(bookings);
        List<ClientBookingResponse> items = bookings.stream()
            .map(b -> toResponse(b, seatNameMap.get(b.getSeatId()), spaceNameMap.get(b.getSpaceId())))
            .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    public ClientBookingResponse detail(String phone, Long bookingId) {
        validatePhone(phone);
        Booking booking = requireMine(phone, bookingId);
        String seatName = buildSeatNameMap(java.util.Collections.singletonList(booking)).get(booking.getSeatId());
        String spaceName = buildSpaceNameMap(java.util.Collections.singletonList(booking)).get(booking.getSpaceId());
        return toResponse(booking, seatName, spaceName);
    }

    @Transactional
    public ClientBookingResponse cancel(String phone, Long bookingId, String cancelReason) {
        validatePhone(phone);
        Booking booking = requireMine(phone, bookingId);
        if (booking.getStatus() != 1) {
            throw new BusinessException(409, "濠电偛顕慨鎾箠鎼达絾顐介柣鏂挎啞婵挳鎮归幁鎺戝闁哄棗绻愯灋闁哄鐏濋顐︽倵鐟欏嫬鈻曠€规洩缍侀、娑橆潩椤掑倹鍋ф繝?");
        }
        if (!booking.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "濠碘槅鍋呭妯尖偓姘煎枤閳ь剛鎳撻崯鍧楊敊韫囨稑绠甸柟鐑樻⒒閿涙稒绻濆▓鍨灈妞ゆ垶鍨瑰Σ鎰攽鐎ｎ亞鐫勯梺缁樏壕顓㈠Υ婵犲洦鐓曟繛鍡樺姉婢ь剛绱?");
        }
        booking.setStatus(3);
        if (StringUtils.hasText(cancelReason)) {
            booking.setCancelReason(cancelReason);
        }
        bookingMapper.updateById(booking);
        String seatName = buildSeatNameMap(java.util.Collections.singletonList(booking)).get(booking.getSeatId());
        String spaceName = buildSpaceNameMap(java.util.Collections.singletonList(booking)).get(booking.getSpaceId());
        return toResponse(booking, seatName, spaceName);
    }

    private Booking requireMine(String phone, Long bookingId) {
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (user == null) {
            throw new BusinessException(404, "濠碘槅鍋呭妯尖偓姘煎枤閳ь剛顢婂▍鏇犵矙婢跺鍚嬮柛娑卞灡閹插ジ姊?");
        }
        Booking booking = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
            .eq(Booking::getId, bookingId)
            .eq(Booking::getUserId, user.getId()));
        if (booking == null) {
            throw new BusinessException(404, "濠碘槅鍋呭妯尖偓姘煎枤閳ь剛顢婂▍鏇犵矙婢跺鍚嬮柛娑卞灡閹插ジ姊?");
        }
        return booking;
    }

    private void checkConflict(Long seatId, LocalDateTime startAt, LocalDateTime endAt) {
        Long count = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
            .eq(Booking::getSeatId, seatId)
            .in(Booking::getStatus, 1, 2)
            .lt(Booking::getStartAt, endAt)
            .gt(Booking::getEndAt, startAt));
        if (count != null && count > 0) {
            throw new BusinessException(409, "闂佽崵濮村ú銏ゅ磿閹绘帩娓婚柛灞惧焹閺嬫棃姊婚崼鐔烘创闁绘稒鎸搁湁闁挎繂鎳愬瓭闂佸憡鍩婄槐鏇㈠箖椤曗偓椤㈡洟鏁傜紒妯活吙缂?");
        }
    }

    private AppUser findOrCreateUser(String phone) {
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getPhone, phone));
        if (user != null) {
            return user;
        }
        AppUser entity = new AppUser();
        entity.setPhone(phone);
        entity.setNickname("闂備焦妞垮鍧楀礉瀹ュ鏄? + phone.substring(phone.length() - 4)");
        entity.setRole(1);
        entity.setStatus(1);
        appUserMapper.insert(entity);
        return entity;
    }

    private String generateBookingNo() {
        return "BK" + LocalDateTime.now().format(BOOKING_NO_TIME) + String.format("%04d", RANDOM.nextInt(10000));
    }

    private String generateConfirmCode() {
        for (int i = 0; i < 5; i++) {
            String code = String.format("%06d", RANDOM.nextInt(1000000));
            Booking exists = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>().eq(Booking::getConfirmCode, code));
            if (exists == null) {
                return code;
            }
        }
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    private Map<Long, String> buildSeatNameMap(List<Booking> bookings) {
        Set<Long> seatIds = bookings.stream().map(Booking::getSeatId).collect(Collectors.toSet());
        if (seatIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>().in(Seat::getId, seatIds));
        return seats.stream().collect(Collectors.toMap(Seat::getId, Seat::getSeatNumber));
    }

    private Map<Long, String> buildSpaceNameMap(List<Booking> bookings) {
        Set<Long> spaceIds = bookings.stream().map(Booking::getSpaceId).collect(Collectors.toSet());
        if (spaceIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Space> spaces = spaceMapper.selectList(new LambdaQueryWrapper<Space>().in(Space::getId, spaceIds));
        return spaces.stream().collect(Collectors.toMap(Space::getId, Space::getName));
    }

    private ClientBookingResponse toResponse(Booking booking, String seatNumber, String spaceName) {
        ClientBookingResponse response = new ClientBookingResponse();
        BeanUtils.copyProperties(booking, response);
        response.setSeatNumber(seatNumber);
        response.setSpaceName(spaceName);
        return response;
    }

    private void validatePhone(String phone) {
        if (!StringUtils.hasText(phone) || !phone.matches("^1\\d{10}$")) {
            throw new BusinessException(400, "闂備礁缍婂褔顢栭崱妞绘敠闁逞屽墴閺屾稑鈻庨幘瀛樻殸闂佺粯顨堥…鍫ヮ敋閿濆牏鐤€閹艰揪绲块幉鐟扳攽椤旂晫绠扮紒鎻掓健閸?");
        }
    }
}



