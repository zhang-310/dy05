package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * AI 文案生成请求 VO
 */
@Data
public class AiCopyGenerateVO {
    private Long viralId;  // 参考爆款ID
    private Long personaId;  // 人设ID
    private String topic;  // 主题
    private String style;  // 风格：humorous / professional / casual / warm
    private Integer length;  // 长度：short(50字) / medium(100字) / long(200字)
    private String keywords;  // 关键词
}
