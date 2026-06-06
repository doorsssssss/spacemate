package com.spacemate.modules.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.spacemate.common.cache.CacheKeys;
import com.spacemate.common.exception.BusinessException;
import com.spacemate.domain.entity.Space;
import com.spacemate.infrastructure.persistence.mapper.SpaceMapper;
import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import com.spacemate.modules.client.service.ClientSpaceService;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClientSpaceServiceImpl implements ClientSpaceService {

    /**
     * 空间数据相对稳定，比“座位可预约状态”更适合缓存。
     *
     * <p>Redis 用作多个后端实例共享的缓存；Caffeine 用作当前 JVM 内的本地缓存。
     * 读取顺序是：先读本地缓存，再读 Redis，最后查数据库。这样热门空间列表可以更快返回。</p>
     */
    private static final Duration SPACE_LIST_TTL = Duration.ofSeconds(60);
    private static final Duration SPACE_DETAIL_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Cache<String, List<ClientSpaceResponse>> spaceListCache;
    private final Cache<Long, ClientSpaceResponse> spaceDetailCache;
    private final SpaceMapper spaceMapper;

    public ClientSpaceServiceImpl(
        StringRedisTemplate redis,
        ObjectMapper objectMapper,
        @Qualifier("clientSpaceListCache") Cache<String, List<ClientSpaceResponse>> spaceListCache,
        @Qualifier("clientSpaceDetailCache") Cache<Long, ClientSpaceResponse> spaceDetailCache,
        SpaceMapper spaceMapper
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.spaceListCache = spaceListCache;
        this.spaceDetailCache = spaceDetailCache;
        this.spaceMapper = spaceMapper;
    }

    @Override
    public List<ClientSpaceResponse> list() {
        List<ClientSpaceResponse> local = spaceListCache.getIfPresent(CacheKeys.SPACE_LIST);
        if (local != null) {
            return local;
        }

        List<ClientSpaceResponse> redisData = readSpaceListFromRedis();
        if (redisData != null) {
            spaceListCache.put(CacheKeys.SPACE_LIST, redisData);
            return redisData;
        }

        /*
         * 客户端空间查询页当前要求“显示全部存在的空间”，所以这里不按 status=1 过滤。
         * 禁用空间可以展示给用户看，但真正查询可预约座位、创建预约时，仍然会调用 requireActiveSpace()，
         * 因此禁用空间不会被预约。
         */
        List<Space> spaces = spaceMapper.selectList(
            new LambdaQueryWrapper<Space>()
                .orderByAsc(Space::getId)
        );

        List<ClientSpaceResponse> data = spaces.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        writeSpaceListToRedis(data);
        spaceListCache.put(CacheKeys.SPACE_LIST, data);
        return data;
    }

    @Override
    public ClientSpaceResponse detail(Long id) {
        ClientSpaceResponse local = spaceDetailCache.getIfPresent(id);
        if (local != null) {
            return local;
        }

        String key = CacheKeys.spaceDetail(id);
        ClientSpaceResponse redisData = readSpaceDetailFromRedis(key);
        if (redisData != null) {
            spaceDetailCache.put(id, redisData);
            return redisData;
        }

        Space space = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
            .eq(Space::getId, id));

        if (space == null) {
            throw new BusinessException(404, "空间不存在");
        }

        ClientSpaceResponse data = toResponse(space);
        writeSpaceDetailToRedis(key, data);
        spaceDetailCache.put(id, data);
        return data;
    }

    @Override
    public Space requireActiveSpace(Long id) {
        Space space = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
            .eq(Space::getId, id)
            .eq(Space::getStatus, 1));
        if (space == null) {
            throw new BusinessException(404, "空间不存在或已停用");
        }
        return space;
    }

    private List<ClientSpaceResponse> readSpaceListFromRedis() {
        String cached = redis.opsForValue().get(CacheKeys.SPACE_LIST);
        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                cached,
                new TypeReference<List<ClientSpaceResponse>>() {
                }
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeSpaceListToRedis(List<ClientSpaceResponse> data) {
        try {
            redis.opsForValue().set(
                CacheKeys.SPACE_LIST,
                objectMapper.writeValueAsString(data),
                SPACE_LIST_TTL
            );
        } catch (Exception ignored) {
            /*
             * 缓存写入失败不能影响接口返回。这里数据库数据已经查询成功，Redis 只是加速层。
             */
        }
    }

    private ClientSpaceResponse readSpaceDetailFromRedis(String key) {
        String cached = redis.opsForValue().get(key);
        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cached, ClientSpaceResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeSpaceDetailToRedis(String key, ClientSpaceResponse data) {
        try {
            redis.opsForValue().set(
                key,
                objectMapper.writeValueAsString(data),
                SPACE_DETAIL_TTL
            );
        } catch (Exception ignored) {
            /*
             * Redis 不可用时，接口仍然返回数据库查询结果，避免缓存故障影响主流程。
             */
        }
    }

    private ClientSpaceResponse toResponse(Space space) {
        ClientSpaceResponse response = new ClientSpaceResponse();
        BeanUtils.copyProperties(space, response);
        return response;
    }
}