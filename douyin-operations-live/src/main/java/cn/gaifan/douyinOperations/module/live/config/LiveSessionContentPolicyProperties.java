package cn.gaifan.douyinOperations.module.live.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播场次内容策略：平台禁止「儿童/未成年人作为直播主体」等主题时，在保存场次时校验标题与简介。
 * 绑定 {@code app.live.content-policy.*}
 * <p>
 * 短视频拍摄、爆款库等非直播链路不在此校验范围内。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.live.content-policy")
public class LiveSessionContentPolicyProperties {

    /** 是否启用「儿童直播」类主题拦截 */
    private boolean childLiveBanEnabled = true;

    /**
     * 标题或简介命中任一子串则拒绝保存（使用组合词，避免误伤「儿童护肤品」等商品表述）。
     */
    private List<String> forbidChildLiveSubstrings = new ArrayList<>(List.of(
            "儿童直播", "少儿直播", "未成年人直播", "未成年直播",
            "童星直播", "小孩直播", "幼儿直播", "宝宝直播",
            "儿童主播", "少儿主播", "未成年主播", "童星主播"
    ));
}
