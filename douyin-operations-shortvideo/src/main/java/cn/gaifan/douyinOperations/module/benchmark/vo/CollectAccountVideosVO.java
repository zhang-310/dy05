package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 采集账号视频请求VO
 */
@Data
public class CollectAccountVideosVO {

    /**
     * 账号ID
     */
    @NotNull(message = "账号ID不能为空")
    private Long benchmarkAccountId;

    /**
     * 最小点赞数（默认1000）
     */
    private Integer minLikeCount = 1000;

    /**
     * 最大视频数（默认50）
     */
    private Integer maxVideos = 50;

    /**
     * Cookie ID（可选）
     */
    private Long cookieId;
}
