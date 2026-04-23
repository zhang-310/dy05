package cn.gaifan.douyinOperations.module.tianapi.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 广告法违禁词检测结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdReviewResultVO {
    /** 是否合规 */
    private boolean compliant;
    /** 检测结果文案：合规/不合规/疑似 */
    private String conclusion;
    /** 违禁词列表 */
    private List<String> words;
}
