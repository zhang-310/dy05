package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;

import java.math.BigDecimal;

/**
 * 混合搜索请求 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HybridSearchRequestVO extends BasicQueryDto {

    private String query;                      // 搜索查询
    private String category;                   // 可选分类过滤
    private String style;                      // 可选风格过滤
    private BigDecimal vectorWeight = new BigDecimal("0.5");    // 向量权重 [0-1]
    private BigDecimal lexicalWeight = new BigDecimal("0.5");   // BM25 权重 [0-1]
    private Integer topK = 20;                 // 返回结果数
    private Boolean withCrossEncoder = false;  // 是否使用 Cross-Encoder 重排
}
