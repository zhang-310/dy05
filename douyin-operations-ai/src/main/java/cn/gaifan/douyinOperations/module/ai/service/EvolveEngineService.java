package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 知识进化引擎服务接口
 */
public interface EvolveEngineService {

    void runEvolution(Long kbId, String evolveAngleHint);

    void runEvolution(Long kbId, String evolveAngleHint, String jobId);

    void runEvolution(Long kbId, String evolveAngleHint, String jobId, List<String> dependsOnTaskNos);

    void tryResumeBlockedTasksAfterCompletion(Long kbId, String completedTaskNo);

    AiEvolveTopic saveTopic(AiEvolveTopic topic);

    void deleteTopic(Long id);

    void deleteTask(Long taskId);

    void cancelTask(Long taskId);

    List<AiEvolveTopic> listTopics(Long kbId, Long accountId, boolean scopeGlobal);

    List<AiEvolveTask> listRecentTasks(int limit);

    /**
     * 自 since（含）起、scoreTotal 有效且 &gt;0 的进化任务，可选按 kbId 过滤（与热力图日历区间对齐）。
     */
    List<AiEvolveTask> listScoredTasksForTrendSince(Timestamp since, Long kbId);

    List<Map<String, Object>> getScoreTrend(int days);

    List<Map<String, Object>> getTopicDistribution();

    String runCompetitorKnowledgeAgent(Long userId, String competitorInfo);

    String runFeedbackDrivenAgent(Long userId);

    String runMultiModalIndexAgent(Long userId, String mediaUrl, String mediaType);

    List<Long> resolveEvolveKbIds();
}
