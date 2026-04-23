package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.shortvideo.vo.ShortVideoBasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SvVideoSearchVO extends ShortVideoBasicQueryDto {
    private Long accountId;
    private Long categoryId;
    private Boolean isViral;
    private String keyword;
    private String startDate;
    private String endDate;
    private Long ownerId;
    private List<Long> ownerIds;
}
