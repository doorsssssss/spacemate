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
import com.spacemate.modules.admin.dto.request.AdminCreateSeatRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSeatRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSeatResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.modules.admin.service.AdminSeatService;
import com.spacemate.modules.admin.service.AdminSpaceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端座位管理业务实现类。
 *
 * <p>座位数据会直接影响客户端“可预约座位查询”。例如管理员禁用某个座位后，
 * 用户下一次查询可预约座位时就不应该再看到这个座位。</p>
 *
 * <p>因此，座位新增、修改、删除成功后，都需要刷新对应空间的可预约座位缓存。</p>
 */
@Service
public class AdminSeatServiceImpl implements AdminSeatService {

    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final AdminSpaceService adminSpaceService;
    private final CacheInvalidationService cacheInvalidationService;

    public AdminSeatServiceImpl(
        SeatMapper seatMapper,
        BookingMapper bookingMapper,
        AdminSpaceService adminSpaceService,
        CacheInvalidationService cacheInvalidationService
    ) {
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.adminSpaceService = adminSpaceService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    /**
     * 查询管理端座位列表。
     *
     * <p>管理端列表不加缓存，优先保证管理员看到最新数据。客户端高频的可预约座位查询，
     * 已经在 {@code ClientSeatServiceImpl} 里单独做 Redis 短缓存。</p>
     */
    @Override
    public PageResponse<AdminSeatResponse> list(Long spaceId, Integer status, long page, long size) {
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<>();
        if (spaceId != null) {
            wrapper.eq(Seat::getSpaceId, spaceId);
        }
        if (status != null) {
            wrapper.eq(Seat::getStatus, status);
        }
        wrapper.orderByDesc(Seat::getId);

        Page<Seat> pageResult = seatMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminSeatResponse> items = pageResult.getRecords().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    /**
     * 在某个启用空间下新增座位。
     *
     * <p>同一个空间下座位编号不能重复。这里先在 Service 层查询判断；更严格的正式项目里，
     * 建议再给数据库加唯一索引，例如 {@code (space_id, seat_number, deleted)}，
     * 这样可以防止并发请求同时插入重复座位编号。</p>
     */
    @Override
    @Transactional
    public AdminSimpleIdResponse create(AdminCreateSeatRequest request) {
        Space space = adminSpaceService.requireById(request.getSpaceId());
        if (space.getStatus() != null && space.getStatus() == 0) {
            throw new BusinessException(400, "不能在已停用空间下新增座位");
        }

        Seat exists = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, request.getSpaceId())
            .eq(Seat::getSeatNumber, request.getSeatNumber()));
        if (exists != null) {
            throw new BusinessException(409, "该空间下座位编号已存在");
        }

        Seat entity = new Seat();
        BeanUtils.copyProperties(request, entity);
        seatMapper.insert(entity);

        /*
         * 新增座位会影响用户查询可预约座位的结果。缓存失效逻辑由 CacheInvalidationService 统一处理，
         * 并且会等当前数据库事务提交成功后再执行。
         */
        cacheInvalidationService.invalidateSeatAvailability(entity.getSpaceId());
        cacheInvalidationService.invalidateSeatDetail(entity.getId());
        return new AdminSimpleIdResponse(entity.getId());
    }

    /**
     * 局部修改一个座位。
     *
     * <p>理论上，前端可以通过传入 spaceId 把座位移动到另一个空间。
     * 如果座位所属空间发生变化，那么旧空间少了一个座位，新空间多了一个座位，
     * 两个空间的可预约座位缓存都需要刷新。</p>
     */
    @Override
    @Transactional
    public AdminUpdateResultResponse update(Long id, AdminUpdateSeatRequest request) {
        Seat entity = requireById(id);
        Long oldSpaceId = entity.getSpaceId();
        Long targetSpaceId = request.getSpaceId() == null ? entity.getSpaceId() : request.getSpaceId();

        Space targetSpace = adminSpaceService.requireById(targetSpaceId);
        if (targetSpace.getStatus() != null && targetSpace.getStatus() == 0) {
            throw new BusinessException(400, "不能把座位移动到已停用空间");
        }

        String targetSeatNumber = request.getSeatNumber() == null
            ? entity.getSeatNumber()
            : request.getSeatNumber();
        Seat duplicate = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, targetSpaceId)
            .eq(Seat::getSeatNumber, targetSeatNumber)
            .ne(Seat::getId, id));
        if (duplicate != null) {
            throw new BusinessException(409, "该空间下座位编号已存在");
        }

        BeanUtils.copyProperties(request, entity, BeanMergeUtils.nullPropertyNames(request));
        int rows = seatMapper.updateById(entity);

        cacheInvalidationService.invalidateSeatAvailability(oldSpaceId);
        cacheInvalidationService.invalidateSeatAvailability(entity.getSpaceId());
        cacheInvalidationService.invalidateSeatDetail(id);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    /**
     * 逻辑删除一个座位，并取消它未来的有效预约。
     *
     * <p>删除座位后，用户不能再预约这个座位。未来的有效预约也需要取消，
     * 但历史记录仍然保留，方便用户查看历史和管理员审计。</p>
     */
    @Override
    @Transactional
    public AdminDeleteResultResponse delete(Long id) {
        Seat entity = requireById(id);
        entity.setDeleted(1);
        entity.setStatus(0);
        entity.setDeletedAt(LocalDateTime.now());
        int rows = seatMapper.updateById(entity);

        bookingMapper.update(new Booking(), new LambdaUpdateWrapper<Booking>()
            .eq(Booking::getSeatId, id)
            .eq(Booking::getStatus, 1)
            .ge(Booking::getStartAt, LocalDateTime.now())
            .set(Booking::getStatus, 3)
            .set(Booking::getCancelReason, "管理员删除座位"));

        cacheInvalidationService.invalidateSeatAvailability(entity.getSpaceId());
        cacheInvalidationService.invalidateSeatDetail(id);
        return new AdminDeleteResultResponse(id, rows > 0);
    }

    /**
     * 查询管理端座位详情。
     */
    @Override
    public AdminSeatResponse detail(Long id) {
        return toResponse(requireById(id));
    }

    /**
     * 根据 ID 查询未被逻辑删除的座位，不存在则抛业务异常。
     *
     * <p>{@code Seat} 使用 MyBatis-Plus 逻辑删除，所以 {@code selectById}
     * 会自动忽略 {@code deleted=1} 的记录。</p>
     */
    @Override
    public Seat requireById(Long id) {
        Seat entity = seatMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "座位不存在");
        }
        return entity;
    }

    private AdminSeatResponse toResponse(Seat seat) {
        AdminSeatResponse response = new AdminSeatResponse();
        BeanUtils.copyProperties(seat, response);
        response.setHasSocket(seat.getHasSocket() != null && seat.getHasSocket() == 1);
        response.setIsQuiet(seat.getIsQuiet() != null && seat.getIsQuiet() == 1);
        return response;
    }
}
