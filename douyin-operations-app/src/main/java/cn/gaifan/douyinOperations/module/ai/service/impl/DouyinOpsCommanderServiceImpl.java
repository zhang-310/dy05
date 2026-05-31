package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.DouyinAgentBenchmarkService;
import cn.gaifan.douyinOperations.module.ai.service.DouyinOpsCommanderService;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountCollectQueueOpsService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectQueueHealthVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DouyinOpsCommanderServiceImpl implements DouyinOpsCommanderService {

    private static final Logger log = LoggerFactory.getLogger(DouyinOpsCommanderServiceImpl.class);

    @Resource
    private DouyinAgentBenchmarkService benchmarkService;
    @Resource
    private AccountCollectQueueOpsService accountCollectQueueOpsService;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private AiKbDocumentRepository documentRepository;
    @Resource
    private AiCallLogRepository callLogRepository;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Override
    public Map<String, Object> buildBrief(Long userId) {
        long started = System.currentTimeMillis();
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.DOUYIN_OPS,
                            FeatureCode.DOUYIN_CONTENT_PLANNING,
                            "抖音运营指挥官简报 userId=" + userId,
                            DeliveryProduct.DOUYIN_OPS
                    ));
        }
        Map<String, Object> benchmark = benchmarkService.runSmokeBenchmark();
        AccountCollectQueueHealthVO queue = accountCollectQueueOpsService.health(userId);
        Map<String, Object> knowledge = buildKnowledgeSnapshot(userId);
        Map<String, Object> aiUsage = buildAiUsageSnapshot();
        List<String> actions = buildActions(benchmark, queue, knowledge, aiUsage);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", "douyin_ops_commander");
        result.put("summary", buildSummary(benchmark, queue, knowledge, aiUsage));
        result.put("benchmark", benchmark);
        result.put("knowledge", knowledge);
        result.put("shortVideoCollectQueue", queue);
        result.put("aiUsage", aiUsage);
        result.put("actions", actions);
        result.put("toolHints", List.of(
                "douyin_ops_commander",
                "kb_rag_search",
                "compliance_check",
                "product_search",
                "script_generate",
                "live_session_query"
        ));
        long elapsedMs = System.currentTimeMillis() - started;
        log.info("douyin_ops_commander.brief completed userId={} durationMs={} slowPath={}",
                userId, elapsedMs, elapsedMs > 120_000);
        return result;
    }

    private Map<String, Object> buildKnowledgeSnapshot(Long userId) {
        List<AiKnowledgeBase> bases = knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("douyin", kbStats(bases, "douyin"));
        result.put("douyinWeigui", kbStats(bases, "douyin_weigui"));
        result.put("allKbCount", bases.size());
        return result;
    }

    private Map<String, Object> kbStats(List<AiKnowledgeBase> bases, String kbName) {
        long kbCount = 0;
        long docs = 0;
        long officialDocs = 0;
        for (AiKnowledgeBase kb : bases) {
            if (!kbName.equals(kb.getKbName())) {
                continue;
            }
            kbCount++;
            docs += documentRepository.countByKbIdAndDeleted(kb.getId(), 0);
            officialDocs += documentRepository.findByKbIdAndSourceTypeAndDeleted(
                    kb.getId(), "douyin_school_official", 0).size();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("kbName", kbName);
        stats.put("kbCount", kbCount);
        stats.put("documents", docs);
        stats.put("officialDocuments", officialDocs);
        stats.put("ready", officialDocs > 0);
        return stats;
    }

    private Map<String, Object> buildAiUsageSnapshot() {
        Timestamp since = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        long referencedCalls = callLogRepository.findWithReferencedChunksSince(since).size();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowDays", 7);
        result.put("referencedCalls", referencedCalls);
        result.put("officialReferenceLoopReady", referencedCalls > 0);
        return result;
    }

    private List<String> buildActions(Map<String, Object> benchmark,
                                      AccountCollectQueueHealthVO queue,
                                      Map<String, Object> knowledge,
                                      Map<String, Object> aiUsage) {
        List<String> actions = new ArrayList<>();
        Number score = benchmark.get("score") instanceof Number n ? n : 0;
        if (score.intValue() < 100) {
            actions.add("触发一次直播话术或短视频脚本生成，确认 referenced_chunk_ids 写入并让 smoke 评测达到满分。");
        }
        if (!Boolean.TRUE.equals(extractReady(knowledge, "douyin"))) {
            actions.add("继续补采 douyin 官方学习中心内容，保证短视频/运营知识库有官方文档。");
        }
        if (!Boolean.TRUE.equals(extractReady(knowledge, "douyinWeigui"))) {
            actions.add("继续补采 douyin_weigui 官方违规内容，作为直播、短视频、千川素材审核硬约束。");
        }
        if (!Boolean.TRUE.equals(aiUsage.get("officialReferenceLoopReady"))) {
            actions.add("检查业务生成链路是否强制检索 douyin 与 douyin_weigui，并在输出展示官方引用。");
        }
        if (!"ok".equals(queue.getHealthStatus())) {
            actions.add("执行 /short-video/account-collect/queue/repair 释放过期租约并重试失败任务。");
        }
        actions.add("多服务器采集时每台机器配置唯一 worker.id、worker.region、账号 Cookie，并共用同一 PostgreSQL 队列表。");
        return actions;
    }

    @SuppressWarnings("unchecked")
    private Boolean extractReady(Map<String, Object> knowledge, String key) {
        Object value = knowledge.get(key);
        if (value instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("ready"));
        }
        return false;
    }

    private String buildSummary(Map<String, Object> benchmark,
                                AccountCollectQueueHealthVO queue,
                                Map<String, Object> knowledge,
                                Map<String, Object> aiUsage) {
        Object score = benchmark.getOrDefault("score", 0);
        return "AI 评测 " + score + " 分，短视频采集队列 " + queue.getHealthStatus()
                + "，官方引用闭环 "
                + (Boolean.TRUE.equals(aiUsage.get("officialReferenceLoopReady")) ? "已产生引用日志" : "待触发生成验证")
                + "，douyin/douyin_weigui 知识库状态见 knowledge。";
    }
}
