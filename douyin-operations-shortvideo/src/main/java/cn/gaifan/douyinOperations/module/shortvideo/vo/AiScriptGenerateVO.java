package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * AI 脚本生成请求 VO
 */
@Data
public class AiScriptGenerateVO {
    private Long viralId;  // 参考爆款ID
    private Long personaId;  // 人设ID
    private String copyText;  // 文案内容
    private String sceneType;  // 场景类型：indoor / outdoor / studio
    private Integer duration;  // 时长（秒）
}
