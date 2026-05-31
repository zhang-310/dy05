package cn.gaifan.douyinOperations.module.abtest.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbExperimentSearchVO extends BasicQueryDto {
    private String experimentType;
    private Integer status;
    private Long ownerId;
    private List<Long> ownerIds;
    private String keyword;
}
