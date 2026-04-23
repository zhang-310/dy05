package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * Agent 投票与共识机制：关键决策由多 Agent 差异化视角并行投票
 */
public interface AgentVotingService {

    /**
     * 对产品分析进行多视角投票
     *
     * @param userPrompt 分析请求
     * @param model      使用的 AI 模型
     * @return 投票共识结果（含 votingDetails）
     */
    String voteOnProductAnalysis(String userPrompt, cn.gaifan.douyinOperations.module.ai.entity.AiModel model);
}
