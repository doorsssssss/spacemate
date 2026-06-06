package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.cache.CacheInvalidationService;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.util.BeanMergeUtils;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import com.spacemate.modules.admin.dto.request.AdminCreateSpaceRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSpaceRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminSpaceResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminSpaceService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理端空间管理业务实现类。
 *
 * <p>这个类负责 space 表的管理端写操作，例如新增空间、修改空间、删除空间。</p>
 *
 * <p>客户端空间列表和空间详情已经做了缓存，所以管理端每次成功修改空间数据后，
 * 都必须刷新对应的客户端缓存。否则管理员刚改完空间名称、营业时间或状态，客户端还可能看到旧数据。</p>
 */
@Service
public class AdminSpaceServiceImpl implements AdminSpaceService {

    private final SpaceMapper spaceMapper;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final CacheInvalidationService cacheInvalidationService;

    public AdminSpaceServiceImpl(
        SpaceMapper spaceMapper,
        SeatMapper seatMapper,
        BookingMapper bookingMapper,
        CacheInvalidationService cacheInvalidationService
    ) {
        this.spaceMapper = spaceMapper;
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    /**
     * 查询管理端空间列表。
     *
     * <p>管理端列表不加缓存，因为管理员操作后通常需要立刻看到最新数据。
     * 这里直接查 MySQL，并使用 MyBatis-Plus 的分页能力。</p>
     */
    @Override
    public PageResponse<AdminSpaceResponse> list(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Space::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(Space::getStatus, status);
        }
        wrapper.orderByDesc(Space::getId);

        Page<Space> pageResult = spaceMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminSpaceResponse> items = pageResult.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    /**
     * 新增空间。
     *
     * <p>主要步骤：</p>
     *
     * <ul>
     *     <li>先校验开放开始时间必须早于开放结束时间。</li>
     *     <li>再校验空间编码 code 不能重复。</li>
     *     <li>插入数据库后，刷新客户端空间缓存和该空间的可预约座位缓存版本。</li>
     * </ul>
     */
    @Override
    @Transactional
    public AdminSimpleIdResponse create(AdminCreateSpaceRequest request) {
        validateOpenTime(request.getOpenStartTime(), request.getOpenEndTime());

        Space exists = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
            .eq(Space::getCode, request.getCode()));
        if (exists != null) {
            throw new BusinessException(409, "空间编码已存在");
        }

        Space entity = new Space();
        BeanUtils.copyProperties(request, entity);
        spaceMapper.insert(entity);

        /*
         * 新增空间会改变客户端空间列表。虽然新空间此时可能还没有座位，
         * 但统一刷新“空间 + 可预约座位”缓存，规则更简单，也不容易漏掉后续扩展场景。
         */
        cacheInvalidationService.invalidateSpaceAndSeatAvailability(entity.getId());
        return new AdminSimpleIdResponse(entity.getId());
    }

    /**
     * 局部修改空间。
     *
     * <p>{@link BeanMergeUtils#nullPropertyNames(Object)} 会找出请求对象里的空字段，
     * 让 {@code BeanUtils.copyProperties} 跳过这些空字段，从而实现“只改前端传来的字段”。</p>
     *
     * <p>开放时间需要额外处理：如果前端只传了开始时间或只传了结束时间，
     * 就要拿数据库里的旧值组合成最终时间段再校验，避免出现开始时间晚于结束时间的脏数据。</p>
     */
    @Override
    @Transactional
    public AdminUpdateResultResponse update(Long id, AdminUpdateSpaceRequest request) {
        Space entity = requireById(id);

        LocalTime finalStart = request.getOpenStartTime() != null
            ? request.getOpenStartTime()
            : entity.getOpenStartTime();
        LocalTime finalEnd = request.getOpenEndTime() != null
            ? request.getOpenEndTime()
            : entity.getOpenEndTime();
        validateOpenTime(finalStart, finalEnd);

        BeanUtils.copyProperties(request, entity, BeanMergeUtils.nullPropertyNames(request));
        int rows = spaceMapper.updateById(entity);

        /*
         * 空间名称、规则、价格、状态会展示在客户端空间页面；
         * 空间状态和开放时间还会影响座位是否可预约，所以两类缓存都要刷新。
         */
        cacheInvalidationService.invalidateSpaceAndSeatAvailability(id);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    /**
     * 逻辑删除空间，并同步处理相关的未来数据。
     *
     * <p>项目使用逻辑删除：数据库行不会物理删除，而是设置 {@code deleted=1}。
     * 这样历史预约和审计信息还能保留。</p>
     *
     * <p>删除空间后，该空间下的座位也要禁用；该空间下未来的有效预约也要取消，
     * 因为空间已经不可用了，这些预约无法继续履约。</p>
     */
    @Override
    @Transactional
    public AdminDeleteResultResponse delete(Long id) {
        Space entity = requireById(id);
        entity.setDeleted(1);
        entity.setStatus(0);
        entity.setDeletedAt(LocalDateTime.now());
        int rows = spaceMapper.updateById(entity);

        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, id));
        if (!seats.isEmpty()) {
            List<Long> seatIds = seats.stream()
                .map(Seat::getId)
                .collect(Collectors.toList());

            seatMapper.update(new Seat(), new LambdaUpdateWrapper<Seat>()
                .in(Seat::getId, seatIds)
                .set(Seat::getDeleted, 1)
                .set(Seat::getStatus, 0)
                .set(Seat::getDeletedAt, LocalDateTime.now()));

            /*
             * 只自动取消未来的有效预约。已经完成、已经取消的历史记录不改，
             * 这样用户历史记录和管理端统计报表不会被破坏。
             */
            bookingMapper.update(new Booking(), new LambdaUpdateWrapper<Booking>()
                .in(Booking::getSeatId, seatIds)
                .eq(Booking::getStatus, 1)
                .ge(Booking::getStartAt, LocalDateTime.now())
                .set(Booking::getStatus, 3)
                .set(Booking::getCancelReason, "管理员删除空间"));
        }

        cacheInvalidationService.invalidateSpaceAndSeatAvailability(id);
        return new AdminDeleteResultResponse(id, rows > 0);
    }

    /**
     * 查询管理端空间详情。
     *
     * <p>管理端详情同样不加缓存，优先保证管理员看到最新数据。</p>
     */
    @Override
    public AdminSpaceResponse detail(Long id) {
        return toResponse(requireById(id));
    }

    /**
     * 根据 ID 查询未被逻辑删除的空间，不存在则抛业务异常。
     *
     * <p>{@link Space#deleted} 标了 MyBatis-Plus 的 {@code @TableLogic}，
     * 所以 {@code selectById} 会自动忽略 {@code deleted=1} 的记录。</p>
     */
    @Override
    public Space requireById(Long id) {
        Space entity = spaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "空间不存在");
        }
        return entity;
    }

    private AdminSpaceResponse toResponse(Space space) {
        AdminSpaceResponse response = new AdminSpaceResponse();
        BeanUtils.copyProperties(space, response);
        return response;
    }

    private void validateOpenTime(LocalTime openStartTime, LocalTime openEndTime) {
        if (openStartTime == null || openEndTime == null || !openStartTime.isBefore(openEndTime)) {
            throw new BusinessException(400, "开放开始时间必须早于开放结束时间");
        }
    }
}