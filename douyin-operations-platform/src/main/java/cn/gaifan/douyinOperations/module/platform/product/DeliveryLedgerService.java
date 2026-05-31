package cn.gaifan.douyinOperations.module.platform.product;

import cn.gaifan.douyinOperations.common.id.Ids;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 产品首单/交付台账写入。
 */
@Service
public class DeliveryLedgerService {

    private final JdbcTemplate jdbcTemplate;

    public DeliveryLedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordDelivery(DeliveryProduct product, String tenantId, String traceId, String status) {
        if (product == null || tenantId == null || traceId == null) {
            return;
        }
        insert(tableFor(product), tenantId, traceId, status != null ? status : "STARTED");
    }

    public void recordVideoInsightDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.VIDEO_INSIGHT, tenantId, traceId, status);
    }

    public void recordShortvideoMakerDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.SHORTVIDEO_MAKER, tenantId, traceId, status);
    }

    public void recordDigitalHumanDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.DIGITAL_HUMAN, tenantId, traceId, status);
    }

    public void recordKnowledgeBaseDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.KNOWLEDGE_BASE, tenantId, traceId, status);
    }

    public void recordDouyinOpsDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.DOUYIN_OPS, tenantId, traceId, status);
    }

    public void recordDramaAiDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.DRAMA_AI, tenantId, traceId, status);
    }

    public void recordPhotoAvatarVideoDelivery(String tenantId, String traceId, String status) {
        recordDelivery(DeliveryProduct.PHOTO_AVATAR_VIDEO, tenantId, traceId, status);
    }

    private static String tableFor(DeliveryProduct product) {
        return switch (product) {
            case VIDEO_INSIGHT -> "gf_video_insight_delivery_workspace_ledger";
            case SHORTVIDEO_MAKER -> "gf_shortvideo_maker_delivery_workspace_ledger";
            case DIGITAL_HUMAN -> "gf_digital_human_delivery_workspace_ledger";
            case KNOWLEDGE_BASE -> "gf_knowledge_base_delivery_workspace_ledger";
            case DOUYIN_OPS -> "gf_douyin_ops_delivery_workspace_ledger";
            case DRAMA_AI -> "gf_drama_ai_delivery_workspace_ledger";
            case PHOTO_AVATAR_VIDEO -> "gf_photo_avatar_video_delivery_workspace_ledger";
        };
    }

    private void insert(String table, String tenantId, String traceId, String status) {
        jdbcTemplate.update(
                "insert into " + table + " (ledger_id, tenant_id, trace_id, status) values (?, ?, ?, ?) on conflict (ledger_id) do nothing",
                Ids.compactUuid("delivery"),
                tenantId,
                traceId,
                status
        );
    }
}
