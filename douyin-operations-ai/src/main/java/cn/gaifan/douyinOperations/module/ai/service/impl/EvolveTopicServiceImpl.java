package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 进化主题服务实现：主题采样、列表、CRUD、去重、分布统计。
 */
@Service
public class EvolveTopicServiceImpl implements EvolveTopicService {

    private static final Logger log = LoggerFactory.getLogger(EvolveTopicServiceImpl.class);

    @Value("${app.ai.evolve.sample-count:10}")
    private int sampleCount = 10;

    @Resource
    private AiEvolveTopicRepository topicRepository;
    @Resource
    private AiEvolveTaskRepository taskRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiEvolveTopic saveTopic(AiEvolveTopic topic) {
        if (topic.getAccountId() != null && topic.getAccountId() > 0) {
            topic.setKbId(null);
        }
        if (topic.getKbId() != null && topic.getKbId() > 0) {
            topic.setAccountId(null);
        }
        topic.setPriority(normalizeTopicPriority(topic.getPriority()));
        return topicRepository.save(topic);
    }

    /** 与前端 P1/P2/P3 一致：仅允许 1–3，历史脏值收敛为 2 */
    private static int normalizeTopicPriority(Integer p) {
        if (p == null || p < 1 || p > 3) {
            return 2;
        }
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTopic(Long id) {
        topicRepository.findById(id).ifPresent(t -> {
            t.setDeleted(1);
            topicRepository.save(t);
        });
    }

    @Override
    public List<AiEvolveTopic> listTopics(Long kbId, Long accountId, boolean scopeGlobal) {
        if (scopeGlobal) {
            return topicRepository.findGlobalTopicsOrderByPriorityAscCreateTimeDesc();
        }
        if (accountId != null && accountId > 0) {
            return topicRepository.findByAccountIdAndStatusAndDeletedOrderByPriorityAsc(accountId, 1, 0);
        }
        if (kbId != null && kbId > 0) {
            List<AiEvolveTopic> specific = topicRepository.findByKbIdAndStatusAndDeletedOrderByPriorityAsc(kbId, 1, 0);
            List<AiEvolveTopic> global = topicRepository.findGlobalTopicsOrderByPriorityAscCreateTimeDesc();
            List<AiEvolveTopic> result = new ArrayList<>(specific);
            result.addAll(global);
            if (!result.isEmpty()) {
                return result;
            }
            // 库内仅有绑定其他 KB 的主题、且不存在 kb_id/account_id 均为 null 的全局行时，合并结果为空；
            // 进化工作台仍需展示可用主题，回退为全部启用主题（与「全部知识库」列表一致）。
            return topicRepository.findByStatusAndDeletedOrderByPriorityAscCreateTimeDesc(1, 0);
        }
        return topicRepository.findByStatusAndDeletedOrderByPriorityAscCreateTimeDesc(1, 0);
    }

    @Override
    public List<Map<String, Object>> getTopicDistribution() {
        List<Object[]> rows = topicRepository.countByCategory();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String cat = row[0] != null ? row[0].toString() : "basic";
            long cnt = row[1] instanceof Number n ? n.longValue() : 0;
            result.add(Map.<String, Object>of("category", cat, "count", cnt));
        }
        return result;
    }

    @Override
    public List<AiEvolveTopic> sampleTopicsWithAttributionWeight(Long kbId, AiKnowledgeBase kb) {
        java.util.Set<Long> hotTopicIds = new java.util.HashSet<>();
        Timestamp since = Timestamp.valueOf(LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay());
        List<AiEvolveTask> highScoreTasks = (kbId != null)
                ? taskRepository.findHighScoreTasksSinceByKbId(kbId, since, 70)
                : taskRepository.findHighScoreTasksSince(since, 70);
        for (AiEvolveTask t : highScoreTasks) {
            String ids = t.getTopicIds();
            if (ids == null || ids.isBlank()) continue;
            try {
                for (String s : ids.replaceAll("[\\[\\]\\s]", "").split(",")) {
                    if (!s.isEmpty()) hotTopicIds.add(Long.parseLong(s.trim()));
                }
            } catch (NumberFormatException e) {
                log.debug("topicIds解析失败: {}", e.getMessage());
            }
        }
        String categoryPattern = resolveCategoryPatternForKb(kb);
        List<AiEvolveTopic> candidates = categoryPattern != null
                ? topicRepository.findTopicsForSamplingByCategoryPrefix(kbId, categoryPattern, PageRequest.of(0, sampleCount * 3))
                : topicRepository.findTopicsForSampling(kbId, PageRequest.of(0, sampleCount * 3));
        if (candidates.isEmpty()) return Collections.emptyList();
        List<AiEvolveTopic> hot = new ArrayList<>(candidates.stream().filter(t -> hotTopicIds.contains(t.getId())).toList());
        List<AiEvolveTopic> rest = new ArrayList<>(candidates.stream().filter(t -> !hotTopicIds.contains(t.getId())).toList());
        Collections.shuffle(hot);
        Collections.shuffle(rest);
        List<AiEvolveTopic> result = new ArrayList<>(hot);
        result.addAll(rest);
        return result.stream().limit(sampleCount).toList();
    }

    @Override
    public boolean isTopicDuplicate(String newTopic, List<AiEvolveTopic> existing, double threshold) {
        if (existing == null || existing.isEmpty()) return false;
        String a = normalizeTopic(newTopic);
        for (AiEvolveTopic t : existing) {
            if (t.getTopic() == null) continue;
            String b = normalizeTopic(t.getTopic());
            if (similarityRatio(a, b) >= threshold) return true;
        }
        return false;
    }

    @Override
    public String resolveCategoryPatternForKb(AiKnowledgeBase kb) {
        if (kb == null) return null;
        String type = kb.getKbType() != null ? kb.getKbType().trim().toLowerCase() : "";
        if ("douyin".equals(type)) return "douyin_%";
        if ("huashu".equals(type)) return "huashu_%";
        if ("zhishi".equals(type)) return "zhishi_%";
        return null;
    }

    private static String normalizeTopic(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static double similarityRatio(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        int maxLen = Math.max(a.length(), b.length());
        int dist = levenshteinDistance(a, b);
        return 1.0 - (double) dist / maxLen;
    }

    private static int levenshteinDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }
}
