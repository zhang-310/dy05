package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * G-1 批量并行生成请求 VO
 */
@Data
public class ParallelGenerateVO {
    /** 要生成的槽位 ID 列表 */
    @NotEmpty(message = "槽位 ID 列表不能为空")
    @Size(max = 50, message = "批量生成槽位数不能超过 50")
    private List<Long> scriptIds;

    /** 模型 ID（可选，不指定时使用默认模型） */
    private Long modelId;
}
