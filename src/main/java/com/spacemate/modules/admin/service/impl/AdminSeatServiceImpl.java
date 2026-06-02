package com.spacemate.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spacemate.common.util.BeanMergeUtils;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.common.api.PageResponse;
import com.spacemate.modules.admin.dto.request.AdminCreateSeatRequest;
import com.spacemate.modules.admin.dto.request.AdminUpdateSeatRequest;
import com.spacemate.modules.admin.dto.response.AdminDeleteResultResponse;
import com.spacemate.modules.admin.dto.response.AdminSeatResponse;
import com.spacemate.modules.admin.dto.response.AdminSimpleIdResponse;
import com.spacemate.modules.admin.dto.response.AdminUpdateResultResponse;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.spacemate.modules.admin.service.AdminSeatService;
import com.spacemate.modules.admin.service.AdminSpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSeatServiceImpl implements AdminSeatService {

    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final AdminSpaceService adminSpaceService;

    public AdminSeatServiceImpl(SeatMapper seatMapper, BookingMapper bookingMapper, AdminSpaceService adminSpaceService) {
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.adminSpaceService = adminSpaceService;
    }

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
        List<AdminSeatResponse> items = pageResult.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(items, page, size, pageResult.getTotal());
    }

    @Transactional
    public AdminSimpleIdResponse create(AdminCreateSeatRequest request) {
        Space space = adminSpaceService.requireById(request.getSpaceId());
        if (space.getStatus() == 0) {
            throw new BusinessException(400, "闂備礁婀遍。浠嬪磻閹剧粯鍊堕柣鎰版涧娴犙囨煃鐟欏嫮澧垫慨濠傤煼瀵爼骞嬪┑鍡橆吋闂備胶顭堥鍡欏垝鎼淬劌鏋侀柕鍫濐槹閺咁剟鎮橀悙闈涗壕闁跨喆鍎茬换娑欏緞鐎ｎ偆顦ㄩ梺鍝勮嫰閿曘儵寮查崼鏇熷€烽柡灞诲劤瀹曟粍绻?");
        }
        Seat exists = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, request.getSpaceId())
            .eq(Seat::getSeatNumber, request.getSeatNumber()));
        if (exists != null) {
            throw new BusinessException(409, "闂佸湱鍘уú鐑藉磼濠婂憛銏ゆ⒑閸涘﹦鎳冮柛銏＄叀瀹曟瑩鏁嶉崟銊ヤ壕婵炴垶顏鍕惰€?");
        }
        Seat entity = new Seat();
        BeanUtils.copyProperties(request, entity);
        seatMapper.insert(entity);
        return new AdminSimpleIdResponse(entity.getId());
    }

    @Transactional
    public AdminUpdateResultResponse update(Long id, AdminUpdateSeatRequest request) {
        Seat entity = requireById(id);
        Long targetSpaceId = request.getSpaceId() == null ? entity.getSpaceId() : request.getSpaceId();
        Space targetSpace = adminSpaceService.requireById(targetSpaceId);
        if (targetSpace.getStatus() == 0) {
            throw new BusinessException(400, "闂備礁婀遍。浠嬪磻閹剧粯鍊堕柣鎰版涧娴犙囨煃鐟欏嫮澧垫慨濠傤煼瀵爼骞嬪┑鍡橆吋闂備胶顭堥鍡欏垝鎼淬劌鏋侀柕鍫濐槹閺咁剟鎮橀悙闈涗壕闁跨喆鍎茬换娑欏緞鐎ｎ偆锛涢梺閫涚┒閸旀垵顕ｉ妸鈺傚仭濞寸厧顕畷婊勭箾?");
        }
        String targetSeatNumber = request.getSeatNumber() == null ? entity.getSeatNumber() : request.getSeatNumber();
        Seat duplicate = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getSpaceId, targetSpaceId)
            .eq(Seat::getSeatNumber, targetSeatNumber)
            .ne(Seat::getId, id));
        if (duplicate != null) {
            throw new BusinessException(409, "Seat number already exists in this space");
        }
        BeanUtils.copyProperties(request, entity, BeanMergeUtils.nullPropertyNames(request));
        int rows = seatMapper.updateById(entity);
        return new AdminUpdateResultResponse(id, rows > 0);
    }

    @Transactional
    public AdminDeleteResultResponse delete(Long id) {
        Seat entity = requireById(id);
        entity.setDeleted(1);
        entity.setStatus(0);
        entity.setDeletedAt(LocalDateTime.now());
        int rows = seatMapper.updateById(entity);
        bookingMapper.update(new Booking(),
            new LambdaUpdateWrapper<Booking>()
                .eq(Booking::getSeatId, id)
                .eq(Booking::getStatus, 1)
                .ge(Booking::getStartAt, LocalDateTime.now())
                .set(Booking::getStatus, 3)
                .set(Booking::getCancelReason, "Seat removed by admin"));
        return new AdminDeleteResultResponse(id, rows > 0);
    }

    public AdminSeatResponse detail(Long id) {
        return toResponse(requireById(id));
    }

    public Seat requireById(Long id) {
        Seat entity = seatMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "闂佸湱鍘уú鐑藉磼濠婂憛銏＄箾閹寸偞灏紒澶嬫尦閹椽濡搁埡浣瑰祶?");
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



