package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 开播准备清单 VO
 * 对应接口设计 3.2.7 GET /sessions/{id}/readiness
 */
@Data
public class LiveReadinessVO {

    /** 是否满足开播条件 */
    private boolean ready;

    /** 各项检查结果 */
    private Checks checks;

    @Data
    public static class Checks {
        private CheckItem products;
        private CheckItem persona;
        private CheckItem scripts;
        private CheckItem compliance;
    }

    @Data
    public static class CheckItem {
        /** 是否通过 */
        private boolean passed;
        /** 数量（products/scripts 使用） */
        private Integer count;
        /** 人设名称（persona 使用） */
        private String personaName;
        /** 提示信息 */
        private String message;
    }
}
