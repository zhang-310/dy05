package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 直播话术响应 VO
 */
@Data
public class LiveScriptVO {

    private Long id;
    private Long sessionId;
    private String scriptContent;
    private Integer sequenceNo;
    private Long executionTime;
    private Integer executed;
    private Timestamp actualExecutionTime;

    // P0-2: 补全 Entity 字段
    private String scriptType;              // 话术类型（opening/product/closing等）
    private String style;                   // 话术风格
    private Boolean aiGenerated;            // 是否 AI 生成
    private Long productId;                 // 关联商品 ID
    private Long aiCallLogId;               // AI 调用日志 ID
    private String generationStatus;        // 生成状态
    private Boolean violationChecked;       // 违规检测状态
    private String violationResult;         // 违规检测结果
    private Integer viewerDelta;            // 观看人数变化
    private Integer interactionDelta;       // 互动量变化
    private Integer conversionDelta;        // 转化量变化
    private Double effectivenessScore;      // 效果评分
    private Integer durationLimitSec;       // 时长上限（秒）
    private String requirement;             // 需求描述
    private Long referencedScriptId;        // 引用的产品话术 ID
    private String referencedScriptSnapshot; // 引用话术快照
    private String approvalStatus;          // 审核状态
    private Long userId;                    // 所属用户 ID
    private String generationPromptHash;    // Prompt 哈希
    private Long abExperimentId;            // A/B 实验 ID
    private Long abVariantId;               // A/B 变体 ID
    private String aiSuggestion;            // AI 建议
    private Long promptTemplateId;          // 提示词模板 ID

    private Timestamp createTime;
    private Timestamp updateTime;
}
