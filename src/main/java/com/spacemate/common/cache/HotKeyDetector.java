package com.spacemate.common.cache;

import com.spacemate.config.CacheProperties;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义 HotKey 探测器。
 *
 * 作用：
 * 1. record(key) 记录一次访问。
 * 2. heat(key) 统计最近窗口内访问次数。
 * 3. level(key) 判断热度等级。
 * 4. ttlDuration(...) 根据热度动态延长缓存 TTL。
 */
@Component
public class HotKeyDetector {

    public enum Level {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }
    //配置 有热点检测开关 阈值 窗口大小
    private final CacheProperties properties;

    //计数数组 用于记录每个key在不同时间片段里的访问次数
    private final Map<String, AtomicIntegerArray> counters = new ConcurrentHashMap<>();

    //当前写的是第几个时间片
    private final AtomicInteger current = new AtomicInteger(0);
    //整个滑动窗口划分段数
    private final int segments;


    public HotKeyDetector(CacheProperties properties) {
        this.properties = properties;
        int segmentSeconds = Math.max(1, properties.getHotkey().getSegmentSeconds());
        int windowSeconds = Math.max(segmentSeconds, properties.getHotkey().getWindowSeconds());
        this.segments = Math.max(1, windowSeconds / segmentSeconds);
    }

    /**
     *记录一次访问
     * @param key
     */
    public void record(String key) {
        if (!properties.getHotkey().isEnabled() || !StringUtils.hasText(key)) {
            //热点检测功能没开 key为空时直接不处理
            return;
        }
        //找到整个key对应的计数数组
        AtomicIntegerArray array = counters.computeIfAbsent(key, ignored -> new AtomicIntegerArray(segments));
        //当前时间片加1
        array.incrementAndGet(current.get());
    }

    /**
     * 计算热度值
     * @param key
     * @return
     */
    public int heat(String key) {
        AtomicIntegerArray array = counters.get(key);
        if (array == null) {
            return 0;
        }
        return sum(array);
    }

    /**
     * 根据热度打等级
     * @param key
     * @return
     */
    public Level level(String key) {
        int heat = heat(key);
        CacheProperties.Hotkey config = properties.getHotkey();

        if (heat >= config.getLevelHigh()) {
            return Level.HIGH;
        }
        if (heat >= config.getLevelMedium()) {
            return Level.MEDIUM;
        }
        if (heat >= config.getLevelLow()) {
            return Level.LOW;
        }
        return Level.NONE;
    }

    /**
     * 根据热度算TTl
     * @param baseTtlSeconds
     * @param key
     * @return
     */
    public Duration ttlDuration(int baseTtlSeconds, String key) {
        return Duration.ofSeconds(ttlSeconds(baseTtlSeconds, Integer.MAX_VALUE, key));
    }

    public Duration ttlDuration(int baseTtlSeconds, int maxTtlSeconds, String key) {
        return Duration.ofSeconds(ttlSeconds(baseTtlSeconds, maxTtlSeconds, key));
    }

    /**
     * 计算ttl核心逻辑
     * @param baseTtlSeconds
     * @param maxTtlSeconds
     * @param key
     * @return
     */
    private int ttlSeconds(int baseTtlSeconds, int maxTtlSeconds, String key) {
        //基础ttl至少为1s
        int safeBaseTtl = Math.max(1, baseTtlSeconds);
        //热点记录没开就直接返回
        if (!properties.getHotkey().isEnabled()) {
            return safeBaseTtl;
        }
        //热点key被加ttl
        int ttl = safeBaseTtl + extendSeconds(level(key));
        ttl = ttl + jitterSeconds(ttl);

        return Math.max(1, Math.min(ttl, maxTtlSeconds));
    }

    /**
     * 根据热度等级 返回额外要增加多少秒ttl
     * @param level
     * @return
     */
    private int extendSeconds(Level level) {
        CacheProperties.Hotkey config = properties.getHotkey();

        return switch (level) {
            case HIGH -> config.getExtendHighSeconds();
            case MEDIUM -> config.getExtendMediumSeconds();
            case LOW -> config.getExtendLowSeconds();
            default -> 0;
        };
    }

    /**
     * 随机抖动
     * @param ttlSeconds
     * @return
     */
    private int jitterSeconds(int ttlSeconds) {
        int jitterPercent = Math.max(0, properties.getHotkey().getJitterPercent());
        //最大能抖动多少秒
        int maxJitter = ttlSeconds * jitterPercent / 100;

        if (maxJitter <= 0) {
            return 0;
        }
        //生成一个随机整数 范围是 0 ~ maxJitter，包含 maxJitter 本身
        return ThreadLocalRandom.current().nextInt(maxJitter + 1);
    }

    /**
     * 滑动窗口
     */
    @Scheduled(fixedRateString = "${spacemate.cache.hotkey.segment-seconds:10}000")
    public void rotate() {
        if (!properties.getHotkey().isEnabled()) {
            return;
        }

        int next = (current.get() + 1) % segments;

        for (AtomicIntegerArray array : counters.values()) {
            array.set(next, 0);
        }

        current.set(next);

        // 清理已经没有热度的 key，避免长期运行后内存堆积。
        counters.entrySet().removeIf(entry -> sum(entry.getValue()) == 0);
    }

    private int sum(AtomicIntegerArray array) {
        int total = 0;
        for (int i = 0; i < array.length(); i++) {
            total += array.get(i);
        }
        return total;
    }
}