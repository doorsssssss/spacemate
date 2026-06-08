package com.spacemate.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.spacemate.modules.client.dto.response.ClientSeatResponse;
import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 统一缓存失效服务。
 *
 * <p>这个类看起来不大，但很关键：所有会影响客户端展示数据的写操作，都应该在数据库写入成功后，
 * 调用这里的方法刷新缓存。</p>
 *
 * <p>为什么不在每个业务 Service 里直接写 Redis 删除逻辑？</p>
 *
 * <ul>
 *     <li>缓存 Key 分散在各个类里容易写错，后续维护也容易漏改。</li>
 *     <li>有些数据同时存在 Redis 缓存和 Caffeine 本地缓存，两层都必须一起清理。</li>
 *     <li>业务 Service 应该表达业务语义，比如“空间变了”“座位可预约状态变了”，而不是关心底层 Redis 细节。</li>
 * </ul>
 */
@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final StringRedisTemplate redis;
    private final Cache<String, List<ClientSpaceResponse>> clientSpaceListCache;
    private final Cache<Long, ClientSpaceResponse> clientSpaceDetailCache;
    private final Cache<String, ClientSeatResponse> clientSeatDetailCache;

    public CacheInvalidationService(
        StringRedisTemplate redis,
        @Qualifier("clientSpaceListCache") Cache<String, List<ClientSpaceResponse>> clientSpaceListCache,
        @Qualifier("clientSpaceDetailCache") Cache<Long, ClientSpaceResponse> clientSpaceDetailCache,
        @Qualifier("clientSeatDetailCache") Cache<String, ClientSeatResponse> clientSeatDetailCache
    ) {
        this.redis = redis;
        this.clientSpaceListCache = clientSpaceListCache;
        this.clientSpaceDetailCache = clientSpaceDetailCache;
        this.clientSeatDetailCache = clientSeatDetailCache;
    }

    /**
     * 座位详情缓存目前是客户端读取路径中的一个小加速层。
     *
     * <p>座位被管理员修改、删除后，详情缓存也要同步清掉，避免前端继续看到旧座位信息。</p>
     */
    public void invalidateSeatDetail(Long seatId) {
        if (seatId == null) {
            return;
        }

        runAfterCommitOrNow(() -> {
            try {
                clientSeatDetailCache.invalidate("spacemate:seat:detail:" + seatId);
                redis.delete("spacemate:seat:detail:" + seatId);
                log.info("cache.invalidate seatDetail seatId={}", seatId);
            } catch (Exception e) {
                log.warn("cache.invalidate seatDetail failed seatId={}", seatId, e);
            }
        });
    }

    /**
     * 当空间被新增、修改、禁用或逻辑删除后，刷新客户端空间列表/详情缓存。
     *
     * <p>客户端空间数据现在有两层缓存：</p>
     *
     * <ul>
     *     <li>Caffeine 本地缓存：速度最快，但只存在于当前 JVM 进程内。</li>
     *     <li>Redis 缓存：多个后端实例之间共享。</li>
     * </ul>
     *
     * <p>两层缓存都要清理。如果只删 Redis，当前进程可能还会从 Caffeine 返回旧数据；
     * 如果只清 Caffeine，其他后端实例又可能从旧 Redis 里重新回填本地缓存。</p>
     */
    public void invalidateClientSpace(Long spaceId) {
        if (spaceId == null) {
            return;
        }

        runAfterCommitOrNow(() -> doInvalidateClientSpace(spaceId));
    }

    private void doInvalidateClientSpace(Long spaceId) {
        clientSpaceListCache.invalidate(CacheKeys.SPACE_LIST);
        clientSpaceDetailCache.invalidate(spaceId);

        try {
            redis.delete(CacheKeys.SPACE_LIST);
            redis.delete(CacheKeys.spaceDetail(spaceId));
            log.info("cache.invalidate space spaceId={}", spaceId);
        } catch (Exception e) {
            /*
             * 缓存失效失败需要记录日志，方便排查 Redis 问题；但不能因为 Redis 删除失败就回滚已经成功的业务写入。
             * 这里 MySQL 才是最终数据源，Redis/Caffeine 只是加速层。
             */
            log.warn("cache.invalidate space failed spaceId={}", spaceId, e);
        }
    }

    /**
     * 通过递增版本号，让某个空间的“可预约座位查询缓存”失效。
     *
     * <p>可预约座位会被很多操作影响：创建预约、取消预约、修改座位、删除座位、禁用空间、删除空间等。
     * 如果用通配符删除 Redis Key，成本高，也不安全。这里使用版本号方案：读缓存时把版本号拼进 Key。</p>
     *
     * <pre>
     * spacemate:seat:available:{spaceId}:v{version}:...
     * </pre>
     *
     * <p>版本号递增后，新请求会生成新的 Key，旧缓存就访问不到了，并会在很短的 TTL 后自然过期。</p>
     */
    public void invalidateSeatAvailability(Long spaceId) {
        if (spaceId == null) {
            return;
        }

        runAfterCommitOrNow(() -> doInvalidateSeatAvailability(spaceId));
    }

    private void doInvalidateSeatAvailability(Long spaceId) {
        try {
            Long version = redis.opsForValue().increment(CacheKeys.seatAvailableVersion(spaceId));
            log.info("cache.invalidate seatAvailability spaceId={} version={}", spaceId, version);
        } catch (Exception e) {
            /*
             * 如果版本号递增失败，最坏情况是用户在 5 秒 TTL 内可能读到旧的可预约座位缓存。
             * 基础版项目里这个风险可接受，比因为 Redis 抖动导致预约/管理操作失败更稳。
             */
            log.warn("cache.invalidate seatAvailability failed spaceId={}", spaceId, e);
        }
    }

    /**
     * 空间信息变化时的组合失效方法。
     *
     * <p>空间变化通常同时影响两类客户端数据：</p>
     *
     * <ul>
     *     <li>客户端空间列表/空间详情。</li>
     *     <li>该空间下的可预约座位查询结果。</li>
     * </ul>
     */
    public void invalidateSpaceAndSeatAvailability(Long spaceId) {
        if (spaceId == null) {
            return;
        }

        runAfterCommitOrNow(() -> {
            doInvalidateClientSpace(spaceId);
            doInvalidateSeatAvailability(spaceId);
        });
    }

    /**
     * 如果当前存在数据库事务，则等事务提交成功后再清缓存；如果没有事务，则立即执行。
     *
     * <p>为什么要等事务提交？</p>
     *
     * <p>如果数据库事务还没提交就先删缓存，另一个请求可能马上查缓存未命中，然后读到数据库里的旧数据，
     * 再把旧数据重新写回 Redis，这就会出现“旧值回填”问题。提交后再清缓存可以避开这个坑。</p>
     */
    private void runAfterCommitOrNow(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
