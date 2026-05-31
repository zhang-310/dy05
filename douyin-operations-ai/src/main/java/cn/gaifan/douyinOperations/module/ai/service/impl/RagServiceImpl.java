package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.RagService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public List<RagRetrieveItemVO> retrieve(Long userId, String query, int topK, String scope) {
        if (knowledgeBaseService == null) {
            log.debug("KnowledgeBaseService 未启用，跳过 RAG 检索");
            return List.of();
        }
        try {
            List<RagRetrieveItemVO> results = knowledgeBaseService.hybridSearchAllKbs(
                    userId, query, topK, scope != null ? scope : "all", null);
            return results != null ? results : List.of();
        } catch (Exception e) {
            log.warn("RAG 检索失败: query={}, scope={}, error={}", query, scope, e.getMessage());
            return List.of();
        }
    }

    @Override
    public String buildRagContext(Long userId, String query, int topK, String scope) {
        List<RagRetrieveItemVO> items = retrieve(userId, query, topK, scope);
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("【参考素材】\n\n");
        for (int i = 0; i < items.size(); i++) {
            RagRetrieveItemVO item = items.get(i);
            sb.append("--- 素材 ").append(i + 1).append(" ---\n");
            if (item.getTitle() != null && !item.getTitle().isBlank()) {
                sb.append("标题：").append(item.getTitle()).append("\n");
            }
            if (item.getContent() != null && !item.getContent().isBlank()) {
                sb.append(item.getContent()).append("\n");
            }
            if (item.getSource() != null && !item.getSource().isBlank()) {
                sb.append("来源：").append(item.getSource()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public List<RagRetrieveItemVO> retrieveByIpType(Long userId, String query, int topK, String ipType) {
        if ("phenomenal".equals(ipType)) {
            return retrieveHotspot(userId, query, topK);
        } else if ("top".equals(ipType)) {
            return retrieveIndustryKnowledge(userId, query, topK);
        }
        return retrieve(userId, query, topK, null);
    }

    @Override
    public List<RagRetrieveItemVO> retrieveHotspot(Long userId, String query, int topK) {
        List<RagRetrieveItemVO> results = new ArrayList<>();
        try {
            List<RagRetrieveItemVO> hotspotResults = retrieve(userId, query, topK, "douyin");
            results.addAll(hotspotResults);

            if (results.size() < topK) {
                List<RagRetrieveItemVO> supplementResults = retrieve(userId, query, topK - results.size(), "huashu");
                results.addAll(supplementResults);
            }
        } catch (Exception e) {
            log.warn("热点追踪检索失败，降级到通用检索: {}", e.getMessage());
            return retrieve(userId, query, topK, null);
        }
        return results;
    }

    @Override
    public List<RagRetrieveItemVO> retrieveIndustryKnowledge(Long userId, String query, int topK) {
        List<RagRetrieveItemVO> results = new ArrayList<>();
        try {
            List<RagRetrieveItemVO> knowledgeResults = retrieve(userId, query, topK, "zhishi");
            results.addAll(knowledgeResults);

            if (results.size() < topK) {
                List<RagRetrieveItemVO> supplementResults = retrieve(userId, query, topK - results.size(), "huashu");
                results.addAll(supplementResults);
            }

            if (results.size() < topK) {
                List<RagRetrieveItemVO> douyinResults = retrieve(userId, query, topK - results.size(), "douyin");
                results.addAll(douyinResults);
            }
        } catch (Exception e) {
            log.warn("行业知识检索失败，降级到通用检索: {}", e.getMessage());
            return retrieve(userId, query, topK, null);
        }
        return results;
    }
}
