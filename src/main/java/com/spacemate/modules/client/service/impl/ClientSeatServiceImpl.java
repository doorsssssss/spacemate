package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.spacemate.common.api.PageResponse;
import com.spacemate.common.cache.CacheKeys;
import com.spacemate.common.cache.HotKeyDetector;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.domain.entity.Booking;
import com.spacemate.domain.entity.Seat;
import com.spacemate.infrastructure.persistence.mapper.BookingMapper;
import com.spacemate.infrastructure.persistence.mapper.SeatMapper;
import com.spacemate.modules.client.dto.response.ClientSeatResponse;
import com.spacemate.modules.client.service.ClientSeatService;
import com.spacemate.modules.client.service.ClientSpaceService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClientSeatServiceImpl implements ClientSeatService {

    private static final Logger log = LoggerFactory.getLogger(ClientSeatServiceImpl.class);

    /**
     * 可预约座位属于变化很快的数据。
     *
     * <p>预约成功、取消预约、管理员修改座位、管理员修改空间状态，都可能立刻影响这个查询结果。
     * 所以这里缓存时间故意设置得很短：既能减少用户反复点击同一时间段造成的数据库压力，
     * 又不会让旧数据在前端停留太久。</p>
     */
    private static final Duration SEAT_AVAILABLE_TTL = Duration.ofSeconds(5);
    private static final int AVAILABLE_REDIS_BASE_TTL_SECONDS = 5;
    private static final int AVAILABLE_REDIS_MAX_TTL_SECONDS = 20;
    private static final int DETAIL_REDIS_BASE_TTL_SECONDS = 30;
    private static final int DETAIL_REDIS_MAX_TTL_SECONDS = 60;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, PageResponse<ClientSeatResponse>> availableCache;
    private final Cache<String, ClientSeatResponse> detailCache;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;
    private final ClientSpaceService clientSpaceService;
    private final HotKeyDetector hotKeyDetector;

    public ClientSeatServiceImpl(
        StringRedisTemplate stringRedisTemplate,
        ObjectMapper objectMapper,
        @Qualifier("clientSeatAvailableCache") Cache<String, PageResponse<ClientSeatResponse>> availableCache,
        @Qualifier("clientSeatDetailCache") Cache<String, ClientSeatResponse> detailCache,
        SeatMapper seatMapper,
        BookingMapper bookingMapper,
        ClientSpaceService clientSpaceService,
        HotKeyDetector hotKeyDetector
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.availableCache = availableCache;
        this.detailCache = detailCache;
        this.seatMapper = seatMapper;
        this.bookingMapper = bookingMapper;
        this.clientSpaceService = clientSpaceService;
        this.hotKeyDetector = hotKeyDetector;
    }

    @Override
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
            throw new BusinessException(400, "??????????");
        }

        /*
         * ?????????????
         *
         * ???????????????????????? Redis ?????????????
         * ?????????????????????????????
         */
        clientSpaceService.requireActiveSpace(spaceId);
        String version = currentSeatAvailableVersion(spaceId);

        String cacheKey = CacheKeys.seatAvailable(
            spaceId,
            version,
            startAt,
            endAt,
            hasSocket,
            isQuiet,
            page,
            size
        );
        hotKeyDetector.record(cacheKey);

        PageResponse<ClientSeatResponse> localCached = availableCache.getIfPresent(cacheKey);
        if (localCached != null) {
            log.info("seat.available source=local key={}", cacheKey);
            return localCached;
        }

        PageResponse<ClientSeatResponse> cached = readAvailableCache(cacheKey);
        if (cached != null) {
            availableCache.put(cacheKey, cached);
            log.info("seat.available source=redis key={}", cacheKey);
            return cached;
        }

        PageResponse<ClientSeatResponse> result = queryAvailableSeats(
            spaceId,
            startAt,
            endAt,
            hasSocket,
            isQuiet,
            page,
            size
        );

        writeAvailableCache(cacheKey, result);
        availableCache.put(cacheKey, result);
        log.info("seat.available source=db key={}", cacheKey);
        return result;
    }
    /**
     * 从 MySQL 查询可预约座位。
     *
     * <p>这个方法保留原来的数据库查询逻辑。把它从 {@link #available(Long, LocalDateTime, LocalDateTime, Integer, Integer, long, long)}
     * 中抽出来，是为了让缓存主流程更清楚：参数校验 -> 构造缓存 Key -> 读缓存 -> 查数据库 -> 写缓存。</p>
     */
    private PageResponse<ClientSeatResponse> queryAvailableSeats(
        Long spaceId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer hasSocket,
        Integer isQuiet,
        long page,
        long size
    ) {

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

        List<Long> seatIds = allSeats.stream()
            .map(Seat::getId)
            .collect(Collectors.toList());

        /*
         * 判断预约时间冲突的标准是：
         *
         * 已有预约开始时间 < 本次查询结束时间
         * 已有预约结束时间 > 本次查询开始时间
         *
         * 这个条件可以覆盖完全包含、左侧重叠、右侧重叠等情况。
         * 当前把 status=1 和 status=2 的预约视为占用座位，已取消的预约不再占用座位。
         */
        List<Booking> conflictBookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
            .in(Booking::getSeatId, seatIds)
            .in(Booking::getStatus, 1, 2)
            .lt(Booking::getStartAt, endAt)
            .gt(Booking::getEndAt, startAt));

        Set<Long> conflictSeatIds = conflictBookings.stream()
            .map(Booking::getSeatId)
            .collect(Collectors.toSet());

        List<Seat> availableSeats = allSeats.stream()
            .filter(seat -> !conflictSeatIds.contains(seat.getId()))
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

    /**
     * 读取某个空间当前的可预约座位缓存版本号。
     *
     * <p>版本号默认从 {@code 0} 开始。只要预约或管理端操作可能影响座位可预约结果，
     * 就会递增这个版本号。新请求会用新版本号生成新的 Redis Key，从而绕开旧缓存，避免使用通配符批量删除。</p>
     */
    private String currentSeatAvailableVersion(Long spaceId) {
        String version = stringRedisTemplate.opsForValue().get(CacheKeys.seatAvailableVersion(spaceId));
        return version == null ? "0" : version;
    }

    /**
     * 从 Redis 读取可预约座位分页结果。
     *
     * <p>Redis 异常或 JSON 反序列化失败都当作缓存未命中处理。缓存层不能影响核心预约流程。</p>
     */
    private PageResponse<ClientSeatResponse> readAvailableCache(String cacheKey) {
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }

        try {
            PageResponse<ClientSeatResponse> fullPage = objectMapper.readValue(
                cached,
                new TypeReference<PageResponse<ClientSeatResponse>>() {
                }
            );
            return fullPage;
        } catch (Exception ignored) {
            // ??????????????
        }

        try {
            PageResponse<Long> skeleton = objectMapper.readValue(
                cached,
                new TypeReference<PageResponse<Long>>() {
                }
            );
            return assembleAvailablePageFromSkeleton(skeleton);
        } catch (Exception e) {
            log.warn("seat.available cache read failed key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * ???????????? Redis?
     *
     * <p>Redis ??????????? ID ??????????????????????????????????</p>
     */
    private void writeAvailableCache(String cacheKey, PageResponse<ClientSeatResponse> result) {
        try {
            List<Long> seatIds = result.getItems() == null
                ? Collections.emptyList()
                : result.getItems().stream().map(ClientSeatResponse::getId).collect(Collectors.toList());
            PageResponse<Long> skeleton = new PageResponse<>(seatIds, result.getPage(), result.getSize(), result.getTotal());
            stringRedisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(skeleton),
                hotKeyDetector.ttlDuration(
                    AVAILABLE_REDIS_BASE_TTL_SECONDS,
                    AVAILABLE_REDIS_MAX_TTL_SECONDS,
                    cacheKey
                )
            );
        } catch (Exception e) {
            log.warn("seat.available cache write failed key={}", cacheKey, e);
        }
    }

    /**
     * ?????????? ID ???????????
     */
    private PageResponse<ClientSeatResponse> assembleAvailablePageFromSkeleton(PageResponse<Long> skeleton) {
        if (skeleton == null || skeleton.getItems() == null || skeleton.getItems().isEmpty()) {
            return new PageResponse<>(
                Collections.emptyList(),
                skeleton == null ? 1 : skeleton.getPage(),
                skeleton == null ? 0 : skeleton.getSize(),
                skeleton == null ? 0 : skeleton.getTotal()
            );
        }

        List<ClientSeatResponse> items = skeleton.getItems().stream()
            .map(this::loadSeatResponse)
            .filter(item -> item != null)
            .collect(Collectors.toList());

        return new PageResponse<>(items, skeleton.getPage(), skeleton.getSize(), skeleton.getTotal());
    }

    /**
     * ????????????????? -> Redis -> ????
     */
    private ClientSeatResponse loadSeatResponse(Long id) {
        if (id == null) {
            return null;
        }

        String cacheKey = CacheKeys.seatDetail(id);

        ClientSeatResponse localCached = detailCache.getIfPresent(cacheKey);
        if (localCached != null) {
            return localCached;
        }

        ClientSeatResponse cached = readDetailCache(cacheKey);
        if (cached != null) {
            detailCache.put(cacheKey, cached);
            return cached;
        }

        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getId, id)
            .eq(Seat::getStatus, 1));
        if (seat == null) {
            return null;
        }

        ClientSeatResponse response = toResponse(seat);
        writeDetailCache(cacheKey, response);
        detailCache.put(cacheKey, response);
        return response;
    }

    /**
     * ???????
     */
    @Override
    public ClientSeatResponse detail(Long id) {
        String cacheKey = CacheKeys.seatDetail(id);
        hotKeyDetector.record(cacheKey);

        ClientSeatResponse localCached = detailCache.getIfPresent(cacheKey);
        if (localCached != null) {
            log.info("seat.detail source=local key={}", cacheKey);
            return localCached;
        }

        ClientSeatResponse cached = readDetailCache(cacheKey);
        if (cached != null) {
            detailCache.put(cacheKey, cached);
            log.info("seat.detail source=redis key={}", cacheKey);
            return cached;
        }

        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getId, id)
            .eq(Seat::getStatus, 1));
        if (seat == null) {
            throw new BusinessException(404, "?????????");
        }
        ClientSeatResponse response = toResponse(seat);
        writeDetailCache(cacheKey, response);
        detailCache.put(cacheKey, response);
        log.info("seat.detail source=db key={}", cacheKey);
        return response;
    }

    /**
     * ? Redis ???????????
     */
    private ClientSeatResponse readDetailCache(String cacheKey) {
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cached, ClientSeatResponse.class);
        } catch (Exception e) {
            log.warn("seat.detail cache read failed key={}", cacheKey, e);
            return null;
        }
    }

    /**
     * ????????? Redis ???????? TTL?
     */
    private void writeDetailCache(String cacheKey, ClientSeatResponse response) {
        try {
            stringRedisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(response),
                hotKeyDetector.ttlDuration(
                    DETAIL_REDIS_BASE_TTL_SECONDS,
                    DETAIL_REDIS_MAX_TTL_SECONDS,
                    cacheKey
                )
            );
        } catch (Exception e) {
            log.warn("seat.detail cache write failed key={}", cacheKey, e);
        }
    }

    /**
     * ?????????
     */
    @Override
    public Seat requireActiveSeat(Long id) {
        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
            .eq(Seat::getId, id)
            .eq(Seat::getStatus, 1));
        if (seat == null) {
            throw new BusinessException(404, "?????????");
        }
        return seat;
    }

    /**
     * ?????? Seat ????? ClientSeatResponse ???
     */
    private ClientSeatResponse toResponse(Seat seat) {
        ClientSeatResponse response = new ClientSeatResponse();
        BeanUtils.copyProperties(seat, response);
        response.setHasSocket(seat.getHasSocket() != null && seat.getHasSocket() == 1);
        response.setIsQuiet(seat.getIsQuiet() != null && seat.getIsQuiet() == 1);
        return response;
    }
}
