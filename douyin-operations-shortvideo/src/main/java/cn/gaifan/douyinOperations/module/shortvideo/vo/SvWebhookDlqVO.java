package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 图生视频 Webhook 投递失败落库（T-5）只读列表项：不含原始 URL，仅指纹与错误摘要。
 */
@Data
public class SvWebhookDlqVO {

    private Long id;
    /** 落库时间（毫秒时间戳） */
    private Long createTimeMs;
    private Long taskId;
    /**
     * 脱敏：webhook URL 的 SHA-256 十六进制串截取前缀 + 「…」，便于运营区分配置而不暴露完整哈希。
     */
    private String webhookUrlFingerprint;
    private Integer lastHttpStatus;
    private Integer attemptCount;
    private String errorPreview;
    private String eventCode;
}
