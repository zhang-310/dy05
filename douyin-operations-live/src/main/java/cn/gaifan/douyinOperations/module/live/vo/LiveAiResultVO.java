package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.util.List;

@Data
public class LiveAiResultVO {

    /** 生成的话术内容 */
    private String content;

    /** 话术类型 */
    private String scriptType;

    /** 违规检测结果（null 表示未检测） */
    private ViolationCheckResult violationCheck;

    /** RAG 参考来源（用于展示与反馈归因） */
    private List<RagRefVO> ragRefs;

    /** 引用的 chunk ID 列表（用于 AI 溯源） */
    private String referencedChunkIds;

    /** 归因归属的场次 ID */
    private Long sessionIdForAttribution;

    /** 归因归属的话术 ID */
    private Long scriptIdForAttribution;

    /** 时长适配度信息（Map，含 fit/actual/limit 等键） */
    private java.util.Map<String, Object> durationFit;

    /** 重复检测信息 */
    private java.util.Map<String, Object> duplicateCheck;

    /** 表演指导列表 */
    private java.util.List<String> performanceGuides;

    /** 质量评分 0-100 */
    private Double qualityScore;

    /** 质量等级（A/B/C/D） */
    private String qualityGrade;

    @Data
    public static class ViolationCheckResult {
        private boolean passed;
        private int violationCount;
        private List<String> violations;
    }

    /** RAG 参考来源单项：文档 ID、chunk ID、标题、内容预览、相关度，便于前端展示与提交反馈 */
    @Data
    public static class RagRefVO {
        private Long docId;
        private Long chunkId;
        private String title;
        private String contentPreview;
        private Double score;
    }
}
