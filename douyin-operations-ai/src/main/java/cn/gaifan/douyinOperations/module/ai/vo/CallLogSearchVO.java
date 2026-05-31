package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 调用日志查询参数（管理员）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallLogSearchVO extends BasicQueryDto {

    /** 调用类型：kb_search、text2img、tts、video 等 */
    private String callType;

    /** 状态：1 成功，0 失败 */
    private Integer status;

    /** 关键词（input_summary 模糊） */
    private String keyword;

    /** 开始时间（yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}
