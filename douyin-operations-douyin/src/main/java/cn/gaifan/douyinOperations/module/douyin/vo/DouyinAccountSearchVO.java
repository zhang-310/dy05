package cn.gaifan.douyinOperations.module.douyin.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 抖音账号搜索 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DouyinAccountSearchVO extends BasicQueryDto {

    private Long ownerId;
    /** 数据范围：可见的用户 ID 列表（由 DataScopeService 填充，null 表示不限制） */
    private List<Long> ownerIds;
    private String accountName;
    private String accountId;
    private Integer status;
}
