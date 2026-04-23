package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cookie查询VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DouyinCookieSearchVO extends BasicQueryDto {

    /**
     * 关键词（Cookie名称或账号名称）
     */
    private String keyword;

    /**
     * 平台
     */
    private String platform;

    /**
     * 是否有效
     */
    private Boolean isValid;

    /**
     * 验证状态
     */
    private String checkStatus;
}
