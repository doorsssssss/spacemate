package com.spacemate.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spacemate.modules.client.dto.response.ClientSpaceResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于 Caffeine 的本地缓存配置类。
 *
 * <p>SpaceMate 当前对“客户端空间数据”使用两层缓存：</p>
 *
 * <ul>
 *     <li>Redis：多个后端实例共享的分布式缓存。</li>
 *     <li>Caffeine：当前 JVM 进程内的本地缓存，速度更快。</li>
 * </ul>
 *
 * <p>本地缓存只适合放相对稳定的数据。空间列表和空间详情变化频率较低，只有管理员新增、修改、禁用、删除空间时才变化，
 * 所以适合放入 Caffeine。座位可预约状态会随着每次预约、取消预约发生变化，不适合放本地缓存，当前只使用 Redis 短 TTL 缓存。</p>
 */
@Configuration
public class CacheConfig {

    /**
     * 客户端空间列表本地缓存。
     *
     * <p>Key：字符串缓存 Key。当前基础版只有一个空间列表 Key，即 {@code CacheKeys.SPACE_LIST}。</p>
     *
     * <p>Value：完整的客户端空间列表响应。</p>
     *
     * <p>容量和过期策略说明：</p>
     *
     * <ul>
     *     <li>{@code maximumSize(100)}：基础版项目空间列表查询条件很少，100 个条目已经足够。</li>
     *     <li>{@code expireAfterWrite(30s)}：本地缓存 TTL 较短，保证管理员修改后客户端能较快看到新数据。</li>
     * </ul>
     */
    @Bean("clientSpaceListCache")
    public Cache<String, List<ClientSpaceResponse>> clientSpaceListCache() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    }

    /**
     * 客户端空间详情本地缓存。
     *
     * <p>Key：空间 ID。</p>
     *
     * <p>Value：单个空间的客户端详情响应。</p>
     *
     * <p>详情缓存容量设置得比列表缓存大一些，因为用户浏览空间时可能反复打开多个空间详情页。
     * 当管理员修改空间时，会通过 {@code CacheInvalidationService} 主动清理这层缓存。</p>
     */
    @Bean("clientSpaceDetailCache")
    public Cache<Long, ClientSpaceResponse> clientSpaceDetailCache() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();
    }
}