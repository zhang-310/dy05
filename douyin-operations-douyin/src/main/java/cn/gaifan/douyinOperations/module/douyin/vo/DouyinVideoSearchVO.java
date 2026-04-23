package cn.gaifan.douyinOperations.module.douyin.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 抖音视频搜索 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DouyinVideoSearchVO extends BasicQueryDto {

    private Long accountId;
    private List<Long> accountIds;  // DataScope 注入：可见账号 ID 列表
    private String videoType;
    private String title;
}
