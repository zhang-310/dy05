package cn.gaifan.douyinOperations.module.slangdict.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SdEntrySearchVO extends BasicQueryDto {
    private String keyword;
    private String category;
    private String usageScene;
    private Integer status;
    private Long userId;
    private Long productId;
    private List<Long> userIds;
}
