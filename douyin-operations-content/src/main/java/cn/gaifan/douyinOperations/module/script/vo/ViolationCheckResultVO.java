package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import java.util.List;

@Data
public class ViolationCheckResultVO {
    private boolean hasViolation;
    private int totalCount;
    private List<ViolationHitVO> violations;

    @Data
    public static class ViolationHitVO {
        private String word;
        private int position;
        private int length;
        private String reason;
        private Integer level;
        private String replacement;
        /** 来源：public=公共库 user=个人库 */
        private String source;
    }
}
