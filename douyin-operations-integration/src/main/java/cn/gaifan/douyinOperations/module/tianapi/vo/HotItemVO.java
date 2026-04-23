package cn.gaifan.douyinOperations.module.tianapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热搜榜单项（统一多平台字段）
 * 扩展字段 link/hotZh/position 由鬼鬼鸭等源提供
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotItemVO {

    /** 4 参数构造（兼容 TianAPI 等原有调用） */
    public HotItemVO(String word, String label, Long hotIndex, String source) {
        this(word, label, hotIndex, source, null, null, null);
    }
    /** 热搜词/话题 */
    private String word;
    /** 标签：新/荐/热 或 排名 */
    private String label;
    /** 热度指数 */
    private Long hotIndex;
    /** 平台来源，如 douyin/toutiao/weibo/network */
    private String source;
    /** 话题链接（鬼鬼鸭等） */
    private String link;
    /** 热度中文展示，如 1,165.1万 */
    private String hotZh;
    /** 排名位置（鬼鬼鸭等） */
    private Integer position;
}
