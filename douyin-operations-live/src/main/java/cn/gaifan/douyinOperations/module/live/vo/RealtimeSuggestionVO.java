package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.util.Map;

/**
 * 实时话术建议（Phase 2.4）
 */
@Data
public class RealtimeSuggestionVO {
    /** 展示分类：switch_script / inject_interaction / danmaku_sentiment 等 */
    private String type;
    private String reason;
    private String suggestion;
    private Long suggestedScriptId;
    private int urgency;        // 1-5

    /**
     * 执行动作，与 {@code POST .../execute-suggestion} 一致：next_slot / jump_slot / inject_interaction
     */
    private String actionType;

    /** 如 jump_slot 时携带 slotIndex；可为 null */
    private Map<String, Object> actionPayload;
}
