package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ViolationCheckBatchResultVO {
    /** key -> 该文本的检测结果 */
    private Map<String, ViolationCheckResultVO> results;
    /** 所有文本的违规总数 */
    private int totalViolations;
}
