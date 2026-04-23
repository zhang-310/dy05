package cn.gaifan.douyinOperations.module.auth.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import cn.gaifan.douyinOperations.common.vo.SearchTimestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户搜索 VO（管理员）；继承 BasicQueryDto 统一分页/排序，按日期范围使用 SearchTimestamp。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthUserSearchVO extends BasicQueryDto {

    private String keyword;
    private String username;
    private String nickname;
    private String mobile;
    private String roleCode;
    private Integer status;

    /** 最后登录时间范围（可选）；前端传 start/end，Service 层用 PredicateUtil.setTimestamp 构建条件 */
    private SearchTimestamp lastLoginAt;
}
