package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;

/**
 * 话术风格分配请求：目标实体 + 用户指纹
 */
@Data
public class ScriptStyleAssignRequest {
    /** product / live_session */
    private String targetEntityType;
    private Long targetEntityId;
    /** 用于去重与一致性，可选，缺省用 userId */
    private String userFingerprint;
}
