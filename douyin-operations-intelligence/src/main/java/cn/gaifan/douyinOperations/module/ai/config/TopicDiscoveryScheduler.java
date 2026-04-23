package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiSearchLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主题自发现调度器：
 * 每周一 07:00 从搜索日志中提取近 7 天高频词，
 * 对尚未在主题池中的词通过 LLM 判断是否值得进化，
 * 通过则以 source=discovered、status=0（待审核）写入 ai_evolve_topic。
 * 管理员在「主题池管理」页面审核后激活（status=1）。
 */
@Component
public class TopicDiscoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(TopicDiscoveryScheduler.class);

    private static final String DISCOVERY_SYSTEM = """
            你是抖音护肤/彩妆直播运营专家。请判断以下搜索词是否值得作为知识进化主题（值得深入研究、可产出有价值的运营洞察）。
            格式：逐行输出，格式为 "词|是|分类" 或 "词|否"，分类从以下选择：
            huashu_product/huashu_live/huashu_interaction/zhishi_skincare/zhishi_makeup/douyin_trend/douyin_strategy
            """;

    @Resource
    private AiEvolveTopicRepository topicRepository;

    @Autowired(required = false)
    private AiSearchLogRepository searchLogRepository;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository modelRepository;

    @Value("${app.ai.topic-discovery.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.topic-discovery.days:7}")
    private int lookbackDays;

    @Value("${app.ai.topic-discovery.top-queries:50}")
    private int topQueries;

    @Value("${app.ai.topic-discovery.min-new-topics:3}")
    private int minNewTopics;

    @Scheduled(cron = "${app.ai.topic-discovery.cron:0 0 7 * * MON}")
    public void discoverTopics() {
        if (!enabled) return;
        if (searchLogRepository == null || llmClient == null || modelRepository == null) {
            log.debug("[TopicDiscovery] 依赖服务未就绪，跳过");
            return;
        }

        try {
            log.info("[TopicDiscovery] 开始主题自发现...");
            Timestamp since = Timestamp.from(Instant.now().minus(lookbackDays, ChronoUnit.DAYS));
            List<String> hotQueries = searchLogRepository.findTopQueriesSince(since, topQueries);

            if (hotQueries == null || hotQueries.isEmpty()) {
                log.info("[TopicDiscovery] 近 {} 天无搜索记录", lookbackDays);
                return;
            }

            Set<String> existingTopics = topicRepository.findByStatusAndDeletedOrderByPriorityAscCreateTimeDesc(1, 0)
                    .stream().map(AiEvolveTopic::getTopic).collect(Collectors.toSet());
            // 也排除已待审核的主题（status=0）
            Set<String> pendingTopics = topicRepository.findAll().stream()
                    .filter(t -> t.getDeleted() == 0 && "discovered".equals(t.getSource()))
                    .map(AiEvolveTopic::getTopic).collect(Collectors.toSet());

            List<String> newCandidates = hotQueries.stream()
                    .filter(q -> q != null && !q.isBlank() && q.length() >= 2 && q.length() <= 50)
                    .filter(q -> !existingTopics.contains(q) && !pendingTopics.contains(q))
                    .limit(30)
                    .collect(Collectors.toList());

            if (newCandidates.isEmpty()) {
                log.info("[TopicDiscovery] 无新候选主题");
                return;
            }

            log.info("[TopicDiscovery] 候选主题 {} 个，调用 LLM 筛选...", newCandidates.size());
            String prompt = "请判断以下搜索词是否值得进化：\n" + String.join("\n", newCandidates);

            var models = modelRepository.findByStatusAndDeleted(1, 0);
            if (models == null || models.isEmpty()) return;

            LlmClient.LlmResponse resp = llmClient.chat(models.get(0), DISCOVERY_SYSTEM, prompt);
            if (!resp.success() || resp.content() == null) {
                log.warn("[TopicDiscovery] LLM 未返回有效结果");
                return;
            }

            int added = 0;
            for (String line : resp.content().split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 2) continue;
                String word = parts[0].trim();
                String judge = parts[1].trim();
                if (!"是".equals(judge) || word.isEmpty()) continue;
                String category = parts.length >= 3 ? parts[2].trim() : "douyin_trend";

                AiEvolveTopic topic = new AiEvolveTopic();
                topic.setTopic(word);
                topic.setCategory(category);
                topic.setPriority(80);
                topic.setSource("discovered");
                topic.setStatus(0); // 待审核
                topicRepository.save(topic);
                added++;
            }

            log.info("[TopicDiscovery] 新增 {} 个待审核主题，请管理员在主题池管理页面审核", added);
        } catch (Exception e) {
            log.error("[TopicDiscovery] 主题自发现异常: {}", e.getMessage(), e);
        }
    }
}
