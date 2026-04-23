package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.util.List;

/**
 * 产品话术结果 VO
 */
@Data
public class ProductScriptResultVO {

    /** 话术内容 */
    private String scriptContent;

    /** 话术类型 */
    private String scriptType;

    /** 话术风格 */
    private String style;

    /** 产品 ID */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /** 人设 ID */
    private Long personaId;

    /** 人设名称 */
    private String personaName;

    /** 生成时间（毫秒） */
    private Long generationTime;

    /** Token 使用量 */
    private Integer tokenUsage;

    /** RAG 参考来源（与 LiveAiResultVO.ragRefs 同结构，供前端展示） */
    private List<LiveAiResultVO.RagRefVO> ragRefs;
}
