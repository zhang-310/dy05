package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphEdge;
import cn.gaifan.douyinOperations.module.ai.entity.AiGraphNode;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphEdgeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphNodeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.GraphExtractorService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 知识图谱实体抽取实现（Phase 5 P0-3）
 */
@Service
public class GraphExtractorServiceImpl implements GraphExtractorService {

    private static final Logger log = LoggerFactory.getLogger(GraphExtractorServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LlmClient llmClient;
    @Resource
    private AiGraphNodeRepository nodeRepository;
    @Resource
    private AiGraphEdgeRepository edgeRepository;
    @Resource
    private AiModelRepository modelRepository;

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void extractFromDocument(Long docId, String content, Long ownerId) {
        if (llmClient == null || content == null || content.length() < 50) return;
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        if (models.isEmpty()) return;

        String text = content.substring(0, Math.min(content.length(), 3000));
        String prompt = """
            请从以下文本中抽取实体和关系，返回 JSON：
            {"entities":[{"type":"product|ingredient|effect|persona|brand|audience|technique","name":"..."}],
             "relations":[{"source":"实体名","target":"实体名","type":"contains|treats|suits|competes_with|enhances"}]}
            仅返回 JSON，不要其他文字。
            文本：""" + text;

        try {
            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是实体关系抽取专家", prompt);
            if (!resp.success() || resp.content() == null || resp.content().isBlank()) return;

            String raw = extractJson(resp.content());
            JsonNode root = MAPPER.readTree(raw);
            JsonNode entities = root.get("entities");
            JsonNode relations = root.get("relations");
            if (entities == null || !entities.isArray()) return;

            Map<String, Long> nameToNodeId = new HashMap<>();
            for (JsonNode e : entities) {
                String type = e.has("type") ? e.get("type").asText() : "unknown";
                String name = e.has("name") ? e.get("name").asText().trim() : null;
                if (name == null || name.isBlank()) continue;

                AiGraphNode node = nodeRepository.findByOwnerIdAndEntityTypeAndEntityNameAndDeleted(ownerId, type, name, 0)
                        .orElse(nodeRepository.findByOwnerIdAndEntityTypeAndEntityNameAndDeleted(0L, type, name, 0).orElse(null));
                if (node != null) {
                    nameToNodeId.put(name, node.getId());
                } else {
                    node = new AiGraphNode();
                    node.setOwnerId(ownerId);
                    node.setEntityType(type);
                    node.setEntityName(name);
                    node.setSourceDocId(docId);
                    node.setConfidence(0.8);
                    node.setDeleted(0);
                    node = nodeRepository.save(node);
                    nameToNodeId.put(name, node.getId());
                }
            }

            if (relations != null && relations.isArray()) {
                Set<String> seen = new HashSet<>();
                for (JsonNode r : relations) {
                    String src = r.has("source") ? r.get("source").asText().trim() : null;
                    String tgt = r.has("target") ? r.get("target").asText().trim() : null;
                    String relType = r.has("type") ? r.get("type").asText() : "related";
                    if (src == null || tgt == null || !nameToNodeId.containsKey(src) || !nameToNodeId.containsKey(tgt)) continue;
                    String key = src + "|" + tgt + "|" + relType;
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    AiGraphEdge edge = new AiGraphEdge();
                    edge.setOwnerId(ownerId);
                    edge.setSourceNodeId(nameToNodeId.get(src));
                    edge.setTargetNodeId(nameToNodeId.get(tgt));
                    edge.setRelationType(relType);
                    edge.setSourceDocId(docId);
                    edge.setDeleted(0);
                    edgeRepository.save(edge);
                }
            }
            log.info("实体抽取完成 docId={}, entities={}, relations={}", docId, nameToNodeId.size(), relations != null ? relations.size() : 0);
        } catch (Exception e) {
            log.warn("实体抽取失败 docId={}: {}", docId, e.getMessage());
        }
    }

    private String extractJson(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s;
    }
}
