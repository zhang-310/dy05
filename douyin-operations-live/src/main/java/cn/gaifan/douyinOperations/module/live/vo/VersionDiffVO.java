package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.util.List;

/**
 * 版本差异展示 VO
 * 用于对比两个版本的差异
 */
@Data
public class VersionDiffVO {

    private Long oldVersionId;
    private Long newVersionId;
    private Integer oldVersionNo;
    private Integer newVersionNo;
    private String oldContent;
    private String newContent;
    private List<String> changedFields;
    private String diffHtml;
    private Integer similarity;
    private Boolean recommendNew;
    private String recommendation;
}
