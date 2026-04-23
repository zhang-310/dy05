package cn.gaifan.douyinOperations.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装，与 BasicQueryDto 对应
 * 统一用于 search 接口返回：total、list、pageNum、pageSize
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;
    /** 当前页数据列表 */
    private List<T> list;
    /** 当前页码（从 0 开始，与 BasicQueryDto.page 一致） */
    private Integer pageNum;
    /** 每页条数 */
    private Integer pageSize;

    public static <T> PageResultVO<T> of(Long total, List<T> list, Integer pageNum, Integer pageSize) {
        return new PageResultVO<>(total, list != null ? list : Collections.emptyList(), pageNum, pageSize);
    }

    // ==================== 显式 getter/setter（Lombok @Data 降级兜底）====================
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
