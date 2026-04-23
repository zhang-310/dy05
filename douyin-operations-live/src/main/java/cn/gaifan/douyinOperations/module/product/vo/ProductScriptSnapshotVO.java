package cn.gaifan.douyinOperations.module.product.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品话术快照返回值对象
 * 用于表示引用话术库的快照记录
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptSnapshotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 快照 ID
     */
    private Long id;

    /**
     * 直播场次 ID
     */
    private Long liveSessionId;

    /**
     * 话术版本 ID
     */
    private Long productScriptVersionId;

    /**
     * 快照内容
     */
    private String contentSnapshot;

    /**
     * 引用时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime referencedAt;

    /**
     * 所有者 ID
     */
    private Long ownerId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
