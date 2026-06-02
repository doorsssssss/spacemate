package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.util.BeanMergeUtils;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSpaceRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSpaceRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminSpaceResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import com.spacemate.modules.admin.service.AdminSpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSpaceServiceImpl implements AdminSpaceService {

    private final SpaceMapper spaceMapper;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;

    public AdminSpaceServiceImpl(SpaceMapper spaceMapper, SeatMapper seatMapper, BookingMapper bookingMapper) {
        this.spaceMapper = spaceMapper;
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
    }

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
        List<AdminSpaceResponse> items = pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    @Transactional
    public AdminSimpleIdResponse create(AdminCreateSpaceRequest request) {
        validateOpenTime(request.getOpenStartTime(), request.getOpenEndTime());
        Space exists = spaceMapper.selectOne(new LambdaQueryWrapper<Space>().eq(Space::getCode, request.getCode()));
        if (exists != null) {
            throw new BusinessException(409, "缂備礁鏈钘壩涚捄銊х＝闁哄稁鍋嗛崹宕団偓鐟版啞瑜板啴鎮洪妸鈺佹嵍?");
        }
        Space entity = new Space();
        BeanUtils.copyProperties(request, entity);
        spaceMapper.insert(entity);
        return new AdminSimpleIdResponse(entity.getId());
    }

    @Transactional
    public AdminUpdateResultResponse update(Long id, AdminUpdateSpaceRequest request) {
        Space entity = requireById(id);
        LocalTime finalStart = request.getOpenStartTime() != null ? request.getOpenStartTime() : entity.getOpenStartTime();
        LocalTime finalEnd = request.getOpenEndTime() != null ? request.getOpenEndTime() : entity.getOpenEndTime();
        validateOpenTime(finalStart, finalEnd);
        BeanUtils.copyProperties(request, entity, BeanMergeUtils.nullPropertyNames(request));
        int rows = spaceMapper.updateById(entity);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    @Transactional
    public AdminDeleteResultResponse delete(Long id) {
        Space entity = requireById(id);
        entity.setDeleted(1);
        entity.setStatus(0);
        entity.setDeletedAt(LocalDateTime.now());
        int rows = spaceMapper.updateById(entity);

        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>().eq(Seat::getSpaceId, id));
        if (!seats.isEmpty()) {
            List<Long> seatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            seatMapper.update(new Seat(),
                new LambdaUpdateWrapper<Seat>()
                    .in(Seat::getId, seatIds)
                    .set(Seat::getDeleted, 1)
                    .set(Seat::getStatus, 0)
                    .set(Seat::getDeletedAt, LocalDateTime.now()));
            bookingMapper.update(new Booking(),
                new LambdaUpdateWrapper<Booking>()
                    .in(Booking::getSeatId, seatIds)
                    .eq(Booking::getStatus, 1)
                    .ge(Booking::getStartAt, LocalDateTime.now())
                    .set(Booking::getStatus, 3)
                    .set(Booking::getCancelReason, "Space removed by admin"));
        }
        return new AdminDeleteResultResponse(id, rows > 0);
    }

    public AdminSpaceResponse detail(Long id) {
        return toResponse(requireById(id));
    }

    public Space requireById(Long id) {
        Space entity = spaceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "缂備礁鏈钘壩涢幐搴ｂ枖鐎广儱鎳愰幗鐘绘煕?");
        }
        return entity;
    }

    private AdminSpaceResponse toResponse(Space space) {
        AdminSpaceResponse response = new AdminSpaceResponse();
        BeanUtils.copyProperties(space, response);
        return response;
    }

    private void validateOpenTime(LocalTime openStartTime, LocalTime openEndTime) {
        if (!openStartTime.isBefore(openEndTime)) {
            throw new BusinessException(400, "闂佽В鍋撻柕澶堝€楅悷鍦偓娈垮枓閸嬫挸鈹戦纰卞剱婵＄偛鍊垮濠氬级閹寸姷鐣辨俊鐐€涢褔鎯冮浣侯洸閹艰揪绲垮▔銏ゆ煛婢跺苯鏋戞俊鐐插€垮?");
        }
    }
}



