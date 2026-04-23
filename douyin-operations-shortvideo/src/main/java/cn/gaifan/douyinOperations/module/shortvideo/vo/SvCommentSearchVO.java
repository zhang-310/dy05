package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.shortvideo.vo.ShortVideoBasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SvCommentSearchVO extends ShortVideoBasicQueryDto {
    private Long videoId;
    private String sentiment;
    private String keyword;
}
