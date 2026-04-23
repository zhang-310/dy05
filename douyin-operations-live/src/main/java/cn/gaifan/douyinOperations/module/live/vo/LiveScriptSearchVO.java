package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 直播话术搜索 VO；继承 BasicQueryDto 统一分页/排序
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LiveScriptSearchVO extends BasicQueryDto {

    private Long sessionId;
    private List<Long> sessionIds;  // DataScope 注入：可见场次 ID 列表
    private Integer executed;
}
