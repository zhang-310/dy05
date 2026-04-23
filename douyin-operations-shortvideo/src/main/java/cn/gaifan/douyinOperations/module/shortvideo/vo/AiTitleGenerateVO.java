package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * AI 标题生成请求 VO
 */
@Data
public class AiTitleGenerateVO {
    private String copyText;  // 文案内容
    private String style;  // 风格：clickbait / professional / creative
    private Integer count;  // 生成数量（默认5个）
}
