package cn.gaifan.douyinOperations.module.tianapi.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文本审核结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextAuditResultVO {
    /** 是否合规 */
    private boolean compliant;
    /** 检测结果文案 */
    private String conclusion;
    /** 不合规提示 */
    private String message;
    /** 违禁内容列表 */
    private List<String> words;
}
