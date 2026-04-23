package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * AI 视频方案生成请求 VO
 */
@Data
public class AiVideoPlanGenerateVO {
    private Long viralId;  // 参考爆款ID
    private String copyText;  // 文案
    private String scriptText;  // 脚本
    private String shootingStyle;  // 拍摄风格：vlog / tutorial / story / product
}
