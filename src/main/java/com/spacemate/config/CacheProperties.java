package com.spacemate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SpaceMate 缓存配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "spacemate.cache")
public class CacheProperties {

    private Hotkey hotkey = new Hotkey();

    @Data
    public static class Hotkey {
        private boolean enabled = true;
        private int windowSeconds = 60;
        private int segmentSeconds = 10;
        private int levelLow = 20;
        private int levelMedium = 80;
        private int levelHigh = 200;
        private int extendLowSeconds = 20;
        private int extendMediumSeconds = 60;
        private int extendHighSeconds = 120;
        private int jitterPercent = 10;
    }
}
