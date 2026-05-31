package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.LlmJudgeService;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryKnowledgeGraphService;
import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import cn.gaifan.douyinOperations.module.ai.service.RerankerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模块核心服务健康指示器：暴露所有可选服务的可用状态
 * 访问路径：GET /actuator/health/ai-services
 */
@Component("aiServicesHealth")
public class AiServicesHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private LlmJudgeService llmJudgeService;

    @Autowired(required = false)
    private VectorService vectorService;

    @Autowired(required = false)
    private SearchService searchService;

    @Autowired(required = false)
    private RerankerService rerankerService;

    @Autowired(required = false)
    private IndustryKnowledgeGraphService knowledgeGraphService;

    @Autowired(required = false)
    private TrendMonitorService trendMonitorService;

    @Autowired(required = false)
    private CompetitorInsightService competitorInsightService;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        int available = 0;
        int total = 8;

        details.put("llmClient", checkComponent("LlmClient", llmClient != null));
        if (llmClient != null) available++;

        details.put("llmJudge", checkComponent("LlmJudgeService", llmJudgeService != null && llmJudgeService.isAvailable()));
        if (llmJudgeService != null && llmJudgeService.isAvailable()) available++;

        boolean vectorOk = vectorService != null;
        details.put("vectorService", checkComponent("VectorService", vectorOk));
        if (vectorOk) available++;

        boolean esOk = searchService != null;
        details.put("searchService", checkComponent("SearchService(ES)", esOk));
        if (esOk) available++;

        boolean rerankerOk = rerankerService != null;
        details.put("rerankerService", checkComponent("RerankerService", rerankerOk));
        if (rerankerOk) available++;

        boolean kgOk = knowledgeGraphService != null && knowledgeGraphService.isAvailable();
        details.put("knowledgeGraph", checkComponent("KnowledgeGraphService", kgOk));
        if (kgOk) available++;

        boolean trendOk = trendMonitorService != null && trendMonitorService.isAvailable();
        details.put("trendMonitor", checkComponent("TrendMonitorService", trendOk));
        if (trendOk) available++;

        boolean competitorOk = competitorInsightService != null;
        details.put("competitorInsight", checkComponent("CompetitorInsightService", competitorOk));
        if (competitorOk) available++;

        details.put("summary", available + "/" + total + " 服务可用");

        if (available >= 5) {
            return Health.up().withDetails(details).build();
        } else if (available >= 3) {
            return Health.status("DEGRADED").withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    private Map<String, String> checkComponent(String name, boolean ok) {
        return Map.of("name", name, "status", ok ? "UP" : "DOWN");
    }
}
