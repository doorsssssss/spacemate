package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.cache.CacheInvalidationService;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.domain.entity.AppUser;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.AppUserMapper;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import com.spacemate.modules.client.dto.request.ClientCreateBookingRequest;
import com.spacemate.modules.client.dto.response.ClientBookingResponse;
import com.spacemate.modules.client.service.ClientBookingService;
import com.spacemate.modules.client.service.ClientSeatService;
import com.spacemate.modules.client.service.ClientSpaceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客户端预约业务实现类。
 *
 * <p>这个类负责用户侧预约相关操作：创建预约、查看我的预约、查看预约详情、取消预约。</p>
 *
 * <p>预约写操作会立刻影响座位可预约状态。比如用户预约成功后，同一个时间段里该座位就不能再被其他用户看到为可预约；
 * 用户取消预约后，该座位又会重新释放。因此创建预约和取消预约后，都需要刷新对应空间的可预约座位缓存。</p>
 */
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
    private final CacheInvalidationService cacheInvalidationService;

    public ClientBookingServiceImpl(
        BookingMapper bookingMapper,
        AppUserMapper appUserMapper,
        ClientSeatService clientSeatService,
        ClientSpaceService clientSpaceService,
        SeatMapper seatMapper,
        SpaceMapper spaceMapper,
        CacheInvalidationService cacheInvalidationService
    ) {
        this.bookingMapper = bookingMapper;
        this.appUserMapper = appUserMapper;
        this.clientSeatService = clientSeatService;
        this.clientSpaceService = clientSpaceService;
        this.seatMapper = seatMapper;
        this.spaceMapper = spaceMapper;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    /**
     * 为当前客户端用户创建预约。
     *
     * <p>这里的校验顺序是有意安排的：</p>
     *
     * <ul>
     *     <li>先校验手机号和预约时间，因为这是最便宜的请求参数校验。</li>
     *     <li>再查找或创建用户，并拦截被禁用的用户。</li>
     *     <li>再校验座位和空间是否存在且启用，避免停用资源被预约。</li>
     *     <li>插入预约前检查时间冲突，防止同一座位同一时间段重复预约。</li>
     *     <li>预约插入成功后，提交事务后刷新可预约座位缓存。</li>
     * </ul>
     */
    @Override
    @Transactional
    public ClientBookingResponse create(String phone, ClientCreateBookingRequest request) {
        validatePhone(phone);
        validateBookingTime(request.getStartAt(), request.getEndAt());

        AppUser user = findOrCreateUser(phone);
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "用户账号已被禁用");
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

        /*
         * 新预约会占用该空间下的一个座位。可预约座位缓存 Key 中包含版本号，递增版本号后，
         * 新查询会使用新的 Key，旧缓存自然访问不到，不需要用通配符扫描 Redis 删除。
         */
        cacheInvalidationService.invalidateSeatAvailability(space.getId());
        return toResponse(booking, seat.getSeatNumber(), space.getName());
    }

    /**
     * 查询当前用户的预约列表。
     *
     * <p>这个接口暂时不加缓存。因为结果是用户维度的数据，并且会随着预约/取消频繁变化。
     * 基础版保持数据库实时查询更简单，也更不容易出现用户数据串缓存的问题。</p>
     */
    @Override
    public PageResponse<ClientBookingResponse> mine(
        String phone,
        Integer status,
        LocalDate startDate,
        LocalDate endDate,
        long page,
        long size
    ) {
        validatePhone(phone);

        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
            .eq(AppUser::getPhone, phone));
        if (user == null) {
            return new PageResponse<>(Collections.emptyList(), page, size, 0);
        }

        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<Booking>()
            .eq(Booking::getUserId, user.getId());
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

        /*
         * booking 表只保存 seatId 和 spaceId，前端展示需要座位编号和空间名称。
         * 这里一次性批量查询座位、空间并转成 Map，避免循环里反复查数据库造成 N+1 查询问题。
         */
        Map<Long, String> seatNameMap = buildSeatNameMap(bookings);
        Map<Long, String> spaceNameMap = buildSpaceNameMap(bookings);
        List<ClientBookingResponse> items = bookings.stream()
            .map(booking -> toResponse(
                booking,
                seatNameMap.get(booking.getSeatId()),
                spaceNameMap.get(booking.getSpaceId())
            ))
            .collect(Collectors.toList());

        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    /**
     * 查询当前用户自己的某一条预约详情。
     *
     * <p>真正的数据隔离在 {@link #requireMine(String, Long)} 里完成：用户只能读取 userId 属于自己的预约，
     * 不能通过猜 bookingId 查看别人的预约。</p>
     */
    @Override
    public ClientBookingResponse detail(String phone, Long bookingId) {
        validatePhone(phone);
        Booking booking = requireMine(phone, bookingId);
        String seatName = buildSeatNameMap(Collections.singletonList(booking)).get(booking.getSeatId());
        String spaceName = buildSpaceNameMap(Collections.singletonList(booking)).get(booking.getSpaceId());
        return toResponse(booking, seatName, spaceName);
    }

    /**
     * 取消一个未来的有效预约。
     *
     * <p>当前基础版只允许取消 status=1 的预约。已经开始的预约不能取消，已经取消过的预约也不能重复取消。
     * 数据库更新成功后，需要刷新该空间的可预约座位缓存，因为座位会重新释放出来。</p>
     */
    @Override
    @Transactional
    public ClientBookingResponse cancel(String phone, Long bookingId, String cancelReason) {
        validatePhone(phone);
        Booking booking = requireMine(phone, bookingId);
        if (!Integer.valueOf(1).equals(booking.getStatus())) {
            throw new BusinessException(409, "只有有效预约可以取消");
        }
        if (!booking.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "已开始的预约不能取消");
        }

        booking.setStatus(3);
        if (StringUtils.hasText(cancelReason)) {
            booking.setCancelReason(cancelReason);
        }
        bookingMapper.updateById(booking);

        cacheInvalidationService.invalidateSeatAvailability(booking.getSpaceId());
        String seatName = buildSeatNameMap(Collections.singletonList(booking)).get(booking.getSeatId());
        String spaceName = buildSpaceNameMap(Collections.singletonList(booking)).get(booking.getSpaceId());
        return toResponse(booking, seatName, spaceName);
    }

    /**
     * 查询并校验某条预约是否属于当前手机号对应的用户。
     *
     * <p>这是客户端预约模块最核心的数据隔离规则：只能访问 {@code booking.user_id}
     * 等于当前用户 ID 的记录。</p>
     */
    private Booking requireMine(String phone, Long bookingId) {
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
            .eq(AppUser::getPhone, phone));
        if (user == null) {
            throw new BusinessException(404, "预约不存在");
        }

        Booking booking = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
            .eq(Booking::getId, bookingId)
            .eq(Booking::getUserId, user.getId()));
        if (booking == null) {
            throw new BusinessException(404, "预约不存在");
        }
        return booking;
    }

    /**
     * 检查目标座位在指定时间段内是否已经被占用。
     *
     * <p>两个时间段冲突的判断条件是：</p>
     *
     * <pre>
     * 已有预约开始时间 < 本次预约结束时间
     * 已有预约结束时间 > 本次预约开始时间
     * </pre>
     *
     * <p>当前 status=1 和 status=2 的预约会占用座位；status=3 表示已取消，不再占用座位。</p>
     */
    private void checkConflict(Long seatId, LocalDateTime startAt, LocalDateTime endAt) {
        Long count = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
            .eq(Booking::getSeatId, seatId)
            .in(Booking::getStatus, 1, 2)
            .lt(Booking::getStartAt, endAt)
            .gt(Booking::getEndAt, startAt));
        if (count != null && count > 0) {
            throw new BusinessException(409, "该座位在当前时间段已被预约");
        }
    }

    /**
     * 根据手机号查找客户端用户；如果是第一次预约，则自动创建一个普通用户。
     *
     * <p>当前客户端基础流程把手机号作为轻量身份标识。首次预约时自动创建用户，
     * 后续预约记录就能稳定关联到同一个 userId。</p>
     */
    private AppUser findOrCreateUser(String phone) {
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
            .eq(AppUser::getPhone, phone));
        if (user != null) {
            return user;
        }

        AppUser entity = new AppUser();
        entity.setPhone(phone);
        entity.setNickname("用户-" + phone.substring(phone.length() - 4));
        entity.setRole(1);
        entity.setStatus(1);
        appUserMapper.insert(entity);
        return entity;
    }

    private String generateBookingNo() {
        return "BK" + LocalDateTime.now().format(BOOKING_NO_TIME) + String.format("%04d", RANDOM.nextInt(10000));
    }

    /**
     * 生成 6 位核销码。
     *
     * <p>基础版通过随机数生成，并做少量重复检查。正式项目里建议给 {@code booking.confirm_code}
     * 加唯一索引，或者按日期/预约 ID 维度生成，进一步降低重复风险。</p>
     */
    private String generateConfirmCode() {
        for (int i = 0; i < 5; i++) {
            String code = String.format("%06d", RANDOM.nextInt(1000000));
            Booking exists = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getConfirmCode, code));
            if (exists == null) {
                return code;
            }
        }
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    private Map<Long, String> buildSeatNameMap(List<Booking> bookings) {
        Set<Long> seatIds = bookings.stream()
            .map(Booking::getSeatId)
            .collect(Collectors.toSet());
        if (seatIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
            .in(Seat::getId, seatIds));
        return seats.stream()
            .collect(Collectors.toMap(Seat::getId, Seat::getSeatNumber));
    }

    private Map<Long, String> buildSpaceNameMap(List<Booking> bookings) {
        Set<Long> spaceIds = bookings.stream()
            .map(Booking::getSpaceId)
            .collect(Collectors.toSet());
        if (spaceIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Space> spaces = spaceMapper.selectList(new LambdaQueryWrapper<Space>()
            .in(Space::getId, spaceIds));
        return spaces.stream()
            .collect(Collectors.toMap(Space::getId, Space::getName));
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
            throw new BusinessException(400, "手机号格式不正确");
        }
    }

    private void validateBookingTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BusinessException(400, "预约时间范围不合法");
        }
        if (!startAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "预约开始时间必须晚于当前时间");
        }
    }
}