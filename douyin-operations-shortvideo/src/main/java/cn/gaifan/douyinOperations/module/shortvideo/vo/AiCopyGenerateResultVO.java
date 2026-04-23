package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CONTENT-03：短视频 AI 文案生成结果（正文 + 违规词检测结果 + 行业/公开规则命中 + 可操作提示）
 */
@Data
public class AiCopyGenerateResultVO {

    private String copy;

    private ViolationCheckResultVO violationCheck;

    /** IndustryComplianceService.checkCompliance 原文 */
    private List<Map<String, Object>> industryCompliance;

    /** 合并后的简短建议（供前端展示） */
    private List<String> suggestions = new ArrayList<>();

    /** 是否存在任一类合规风险 */
    private boolean hasComplianceRisk;
}
