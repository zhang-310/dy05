package cn.gaifan.douyinOperations.module.auth.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源搜索 VO（分页、条件）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthResourceSearchVO extends BasicQueryDto {

    /** 类型：menu/api/button */
    private String resourceType;
    private String module;
    private String resourceCode;
    private String resourceName;
    /** 父资源 ID，用于按父节点筛选子资源 */
    private Long parentId;
}
