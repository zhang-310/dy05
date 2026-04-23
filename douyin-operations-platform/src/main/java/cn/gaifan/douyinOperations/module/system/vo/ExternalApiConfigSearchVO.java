package cn.gaifan.douyinOperations.module.system.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部 API 配置查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExternalApiConfigSearchVO extends BasicQueryDto {

    /** 分类过滤：ai / data / media / sms */
    private String category;

    /** 健康状态过滤：healthy / degraded / down */
    private String healthStatus;

    /** 是否启用 */
    private Boolean isEnabled;

    /** 关键词（模糊匹配 providerCode / providerName） */
    private String keyword;
}
