package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;

import java.util.List;

/**
 * 违规词替换建议请求 VO
 */
@Data
public class ViolationReplacementRequestVO {

    /** 原始文本 */
    private String text;

    /** 违规词列表 */
    private List<String> violationWords;

    /** 上下文信息（可选，帮助 AI 更好地理解语境） */
    private String context;

    /** 话术类型（可选：opening/product/transition/closing） */
    private String scriptType;
}
