package cn.gaifan.douyinOperations.module.guiguiya.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鬼鬼鸭 API 配置（免费抖音热搜）
 * 文档：http://api.guiguiya.com/api/hotlist/dy
 */
@Data
@Component
@ConfigurationProperties(prefix = "guiguiya")
public class GuiguiyaProperties {

    /** 是否启用鬼鬼鸭抖音热搜 */
    private boolean enabled = true;

    /** 抖音热搜接口 URL */
    private String douyinHotUrl = "http://api.guiguiya.com/api/hotlist/dy";

    /** 缓存时间（分钟） */
    private int hotCacheMinutes = 5;

    public boolean isConfigured() {
        return enabled && douyinHotUrl != null && !douyinHotUrl.isBlank();
    }
}
