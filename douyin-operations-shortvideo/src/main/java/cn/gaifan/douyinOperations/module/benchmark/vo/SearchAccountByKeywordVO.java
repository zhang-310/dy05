package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按关键词搜索账号请求VO
 */
@Data
public class SearchAccountByKeywordVO {

    /**
     * 搜索关键词
     */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /**
     * 最小粉丝数（默认5万）
     */
    private Long minFanCount = 50000L;

    /**
     * 最大结果数（默认20）
     */
    private Integer maxResults = 20;

    /**
     * Cookie ID（可选，不指定则自动选择）
     */
    private Long cookieId;
}
