package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSaveVO;

/**
 * 知识源管理服务
 */
public interface KnowledgeSourceService {

    PageResultVO<AiKnowledgeSource> search(KnowledgeSourceSearchVO vo);

    AiKnowledgeSource getById(Long id);

    Long save(KnowledgeSourceSaveVO vo);

    void delete(Long id);

    void updateIndexStats(Long id, int fileCount, int indexCount);
}
