package cn.gaifan.douyinOperations.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 产品话术更新事件（保存/回滚时发布，供效果归因、缓存刷新等监听）
 */
public class ProductScriptUpdatedEvent extends ApplicationEvent {

    private final Long scriptId;
    private final Long productId;
    private final String scriptType;
    private final String style;
    private final Long userId;

    public ProductScriptUpdatedEvent(Object source, Long scriptId, Long productId, String scriptType, String style, Long userId) {
        super(source);
        this.scriptId = scriptId;
        this.productId = productId;
        this.scriptType = scriptType;
        this.style = style;
        this.userId = userId;
    }

    public Long getScriptId() { return scriptId; }
    public Long getProductId() { return productId; }
    public String getScriptType() { return scriptType; }
    public String getStyle() { return style; }
    public Long getUserId() { return userId; }
}
