package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 采纳实时建议后的执行入参（LIVE-02）
 */
@Data
public class SuggestionExecuteVO {

    @NotNull
    private Long liveSessionId;

    /**
     * next_slot | jump_slot | inject_interaction
     */
    @NotBlank
    private String actionType;

    /**
     * jump_slot 必填 slotIndex（整数）；其余可为空
     */
    private Map<String, Object> actionPayload;

    /**
     * manual（默认）| auto | system —— 审计用；自动策略调用时应传 auto
     */
    private String executionSource;
}
