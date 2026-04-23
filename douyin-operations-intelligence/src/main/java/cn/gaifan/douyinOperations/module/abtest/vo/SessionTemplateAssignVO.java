package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 直播场次模板 A/B：随机到的变体及应绑定的 {@code live_session_template.id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionTemplateAssignVO {
    private Long experimentId;
    private Long variantId;
    private Long templateId;
    /** A / B，写入 live_session.template_ab_tag */
    private String variantType;
}
