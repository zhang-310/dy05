package cn.gaifan.douyinOperations.module.script.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptSearchVO extends BasicQueryDto {
    private String keyword;
    private String category;
    /** 来源筛选：manual/live/ai */
    private String source;
    private Integer status;
    private Long userId;
    private List<Long> userIds;
}
