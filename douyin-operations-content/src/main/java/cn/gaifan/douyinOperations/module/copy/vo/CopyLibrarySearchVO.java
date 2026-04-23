package cn.gaifan.douyinOperations.module.copy.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CopyLibrarySearchVO extends BasicQueryDto {
    private String keyword;
    private String title;
    private String category;
    private String tags;
    private Integer status;
    private Long userId;
    private List<Long> userIds; // DataScope 注入
}
