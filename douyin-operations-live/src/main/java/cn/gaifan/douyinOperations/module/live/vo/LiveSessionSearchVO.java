package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 直播场次搜索 VO；继承 BasicQueryDto 统一分页/排序
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LiveSessionSearchVO extends BasicQueryDto {

    private Long userId;
    /** 数据范围：可见用户 ID 列表（由 Controller 按角色注入） */
    private java.util.List<Long> userIds;
    private Integer status;
    private String keyword;
    private String liveTitle;
}
