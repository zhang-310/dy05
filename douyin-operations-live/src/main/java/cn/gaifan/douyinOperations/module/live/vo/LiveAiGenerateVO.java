package cn.gaifan.douyinOperations.module.live.vo;

    
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiveAiGenerateVO {

    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;

    /** 人设 ID（可选，不传则使用场次关联的人设） */
    private Long personaId;

    /** 商品 ID（product 类型话术必传） */
    private Long productId;

    /** 话术风格：professional / friendly / passionate / seeding / promotion */
    private String style;

    /** 额外提示词 */
    private String extraPrompt;

    /** 转场话术：来源产品名（transition 类型用） */
    private String fromProductName;

    /** 转场话术：目标产品名（transition 类型用） */
    private String toProductName;

    /** 需求/意图：开场白/产品介绍/促单/转场/收尾/互动引导（逐段生成时用） */
    private String requirement;

    /** 时长上限（秒），0=不限制（逐段生成时用） */
    private Integer durationLimitSec;

    /** 产品分类（hot/profit/loss/flat/control），用于话术时长自动调整；可由 LiveProduct 或 inferProductType 填充 */
    private String productType;

    /** 是否使用话术知识库参考（RAG），默认 true */
    private Boolean useKbRef;

    /** 指定模型 ID（可选，不指定则用默认模型） */
    private Long modelId;

    /** 热词关键词列表（可选，用于话术优化） */
    private java.util.List<String> hotKeywords;

    /** 槽位索引（逐段生成时用，指定当前是第几个槽位） */
    private Integer slotIndex;

    /** A/B 实验 ID（可选） */
    private Long abExperimentId;

    /** A/B 变体 ID（可选） */
    private Long abVariantId;

    /** 指定 Prompt 模板 ID */
    private Long promptTemplateId;

    /** 前置槽位上下文（用于连贯生成） */
    private String priorSlotsContext;

    /** 素材类型 */
    private String materialType;

    /** IP 类型（用于定制生成风格） */
    private String ipType;

    /** 时间段（开场/中场/收尾） */
    private String timeSlot;

    /** 话术模块类型（warmup/product/interaction/promotion/closing 等） */
    private String scriptModule;

    /** 留存策略 */
    private String retentionStrategy;

    /** 互动强度级别（low/medium/high） */
    private String interactionLevel;
}


