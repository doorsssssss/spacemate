package com.spacemate.common.cache;

import java.time.LocalDateTime;

/**
 * Redis 缓存 Key 统一生成类。
 *
 * <p>所有缓存 Key 都集中放在这里生成，目的是让“读缓存”和“删缓存/失效缓存”使用同一套命名规则。
 * 如果各个 Service 自己手写字符串，很容易出现一个地方写成 {@code spacemate:seat:available:...}，
 * 另一个地方删除时写成了相似但不完全一致的 Key，最后导致缓存失效失败。</p>
 */
public final class CacheKeys {

    /**
     * 客户端空间列表缓存 Key。
     *
     * <p>当前客户端空间列表表示“客户端页面要展示的所有未被逻辑删除的空间”。
     * 如果以后 {@code ClientSpaceResponse} 的字段结构发生较大变化，可以把后缀从 {@code v1}
     * 升级成 {@code v2}，避免新代码误读 Redis 里旧结构的 JSON。</p>
     */
    public static final String SPACE_LIST = "spacemate:space:list:v1";

    private CacheKeys() {
    }

    /**
     * 客户端空间详情缓存 Key。
     *
     * <p>空间详情比座位可预约状态稳定得多，所以可以使用稍长 TTL，并且可以再放一层本地缓存。
     * 管理端修改空间基础信息后，需要删除这个 Key。</p>
     */
    public static String spaceDetail(Long spaceId) {
        return "spacemate:space:detail:" + spaceId + ":v1";
    }

    /**
     * 某个空间的“可预约座位查询缓存版本号”Key。
     *
     * <p>这里不使用 Redis 通配符批量删除可预约座位缓存。因为线上 Redis 扫描大量 Key 成本高，
     * 也容易误删。更稳的做法是：每次预约、取消预约、座位变更、空间变更后，把这个版本号加一。
     * 新查询会带着新版本号生成新的缓存 Key，旧缓存自然失效，等待短 TTL 到期自动删除。</p>
     */
    public static String seatAvailableVersion(Long spaceId) {
        return "spacemate:seat:available:version:" + spaceId;
    }

    /**
     * 单个座位详情缓存 Key。
     *
     * <p>座位详情比列表更细，适合单独放一个较短 TTL 的缓存，避免管理员修改座位信息后前端频繁回源数据库。</p>
     */
    public static String seatDetail(Long seatId) {
        return "spacemate:seat:detail:" + seatId;
    }

    /**
     * 客户端“查询某时间段可预约座位”的缓存 Key。
     *
     * <p>这个 Key 必须包含所有会影响查询结果的条件：空间、时间段、插座筛选、安静区筛选、分页参数、版本号。
     * 如果漏掉任何一个条件，不同请求就可能共用同一个缓存结果，导致前端看到错误的座位数据。</p>
     */
    public static String seatAvailable(
        Long spaceId,
        String version,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer hasSocket,
        Integer isQuiet,
        long page,
        long size
    ) {
        return "spacemate:seat:available:"
            + spaceId
            + ":v" + version
            + ":start:" + startAt
            + ":end:" + endAt
            + ":socket:" + nullToAll(hasSocket)
            + ":quiet:" + nullToAll(isQuiet)
            + ":page:" + page
            + ":size:" + size;
    }

    /**
     * 评论列表版本号 Key。
     *
     * <p>评论新增、评论点赞、评论取消点赞后，递增版本号即可让旧缓存自然失效。</p>
     */
    public static String commentListVersion(Long spaceId) {
        return "spacemate:comment:list:version:" + spaceId;
    }

    /**
     * 客户端空间评论列表的热度统计 Key。
     *
     * <p>当前评论列表还没有单独做缓存，但先把命名统一起来，后续如果要给评论列表补 Redis /
     * Caffeine 缓存时，可以直接沿用同一套 Key 规则，不需要再改一轮。</p>
     */
    public static String commentList(Long spaceId, long page, long size) {
        return "spacemate:comment:list:" + spaceId + ":page:" + page + ":size:" + size;
    }

    /**
     * 带版本号的评论列表缓存 Key。
     *
     * <p>评论新增、点赞、取消点赞后，只要递增版本号，旧缓存就会自动失效，不需要批量删除 Redis Key。</p>
     */
    public static String commentList(Long spaceId, String version, long page, long size) {
        return "spacemate:comment:list:" + spaceId + ":v" + version + ":page:" + page + ":size:" + size;
    }

    private static String nullToAll(Object value) {
        return value == null ? "all" : String.valueOf(value);
    }
}
