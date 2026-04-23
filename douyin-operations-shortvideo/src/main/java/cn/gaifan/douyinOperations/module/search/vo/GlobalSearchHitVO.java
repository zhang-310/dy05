package cn.gaifan.douyinOperations.module.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全局搜索单条命中（前端用 path + 角色前缀拼完整路由）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchHitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** LIVE_SESSION | PRODUCT | SCRIPT | SHORT_VIDEO */
    private String kind;
    private Long id;
    private String title;
    private String subtitle;
    /** 相对路径，不含 /admin 等前缀，如 live/sessions/12 */
    private String path;
}
