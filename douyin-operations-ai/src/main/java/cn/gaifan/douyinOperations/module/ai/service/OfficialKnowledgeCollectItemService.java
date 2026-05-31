package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiOfficialKnowledgeCollectItem;
import cn.gaifan.douyinOperations.module.ai.vo.OfficialKnowledgeCollectSearchVO;

import java.util.Map;

public interface OfficialKnowledgeCollectItemService {

    PageResultVO<AiOfficialKnowledgeCollectItem> search(OfficialKnowledgeCollectSearchVO vo);

    Map<String, Object> summary();

    void markDiscovered(CollectItemEvent event);

    void markIndexed(CollectItemEvent event);

    void markSkipped(CollectItemEvent event);

    void markFailed(CollectItemEvent event);

    record CollectItemEvent(
            String sourceType,
            String sourceSite,
            String sourceUrl,
            String sourceId,
            String title,
            String category,
            String topicCode,
            String targetKbName,
            Long targetKbId,
            Long docId,
            boolean violation,
            int imageCount,
            int videoCount,
            int imageTextCount,
            int videoTextCount,
            long officialUpdateTimestamp,
            String metadata,
            String error
    ) {
    }
}
