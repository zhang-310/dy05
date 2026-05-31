package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.DouyinAgentBenchmarkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DouyinAgentBenchmarkServiceImpl implements DouyinAgentBenchmarkService {

    @Resource private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource private AiKbDocumentRepository documentRepository;
    @Resource private AiTaskModelConfigRepository taskModelConfigRepository;
    @Resource private AiCallLogRepository callLogRepository;

    @Override
    public Map<String, Object> runSmokeBenchmark() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(checkKbHasOfficialDocs("douyin", 100));
        checks.add(checkKbHasOfficialDocs("douyin_weigui", 50));
        checks.add(checkTaskModel("short_video_script"));
        checks.add(checkTaskModel("copy_processing"));
        checks.add(checkRecentReferenceCoverage());

        long passed = checks.stream().filter(c -> Boolean.TRUE.equals(c.get("passed"))).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("benchmark", "douyin_agent_smoke");
        result.put("total", checks.size());
        result.put("passed", passed);
        result.put("failed", checks.size() - passed);
        result.put("score", checks.isEmpty() ? 0 : Math.round(passed * 100.0 / checks.size()));
        result.put("cases", listBuiltinCases());
        result.put("checks", checks);
        return result;
    }

    @Override
    public List<Map<String, Object>> listBuiltinCases() {
        return List.of(
                caseRow("live_violation_rule", "直播违规红线", "必须检索 douyin_weigui 并返回官方规则引用"),
                caseRow("short_video_script", "短视频脚本策划", "必须检索 douyin + douyin_weigui 并返回官方引用"),
                caseRow("qianchuan_material_review", "千川素材审核", "必须使用违规库做硬约束"),
                caseRow("product_live_script", "商品直播话术", "必须结合商品角色、时长策略和官方规则"),
                caseRow("attribution_feedback", "效果归因闭环", "生成调用必须写入 referenced_chunk_ids 和业务对象关联")
        );
    }

    private Map<String, Object> checkKbHasOfficialDocs(String kbName, long minDocs) {
        long docs = knowledgeBaseRepository.findAll().stream()
                .filter(k -> k.getDeleted() == 0 && kbName.equals(k.getKbName()))
                .mapToLong(k -> documentRepository.findByKbIdAndSourceTypeAndDeleted(
                        k.getId(), "douyin_school_official", 0).size())
                .sum();
        return check("official_kb_" + kbName, docs >= minDocs,
                "官方知识库 " + kbName + " 文档数 " + docs + " / 最低 " + minDocs,
                Map.of("kbName", kbName, "officialDocs", docs, "minDocs", minDocs));
    }

    private Map<String, Object> checkTaskModel(String taskCode) {
        boolean present = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(taskCode, 1, 0).isPresent();
        return check("task_model_" + taskCode, present,
                "任务模型配置 " + taskCode + (present ? " 已启用" : " 缺失或未启用"),
                Map.of("taskCode", taskCode));
    }

    private Map<String, Object> checkRecentReferenceCoverage() {
        Timestamp since = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        long withRefs = callLogRepository.findWithReferencedChunksSince(since).size();
        boolean passed = withRefs > 0;
        return check("recent_reference_coverage", passed,
                passed ? "最近 7 天已有可归因引用日志" : "最近 7 天还没有 referenced_chunk_ids，需触发一次生成验证",
                Map.of("days", 7, "withReferencedChunks", withRefs));
    }

    private static Map<String, Object> check(String code, boolean passed, String message, Map<String, Object> detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("passed", passed);
        row.put("message", message);
        row.put("detail", detail);
        return row;
    }

    private static Map<String, Object> caseRow(String code, String name, String expectation) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", code);
        row.put("name", name);
        row.put("expectation", expectation);
        return row;
    }
}
