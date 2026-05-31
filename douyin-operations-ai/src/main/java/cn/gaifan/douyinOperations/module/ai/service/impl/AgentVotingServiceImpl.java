package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.AgentVotingService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Agent 投票与共识机制实现
 * <p>
 * 对 ProductAnalyst 节点启动 3 个差异化视角 Agent 并行投票:
 * - Agent-A: 数据驱动分析师（GMV + 转化率优先）
 * - Agent-B: 用户体验分析师（互动率 + 弹幕情绪优先）
 * - Agent-C: 品牌策略分析师（复购率 + 长期品牌价值优先）
 * <p>
 * 投票策略: majority（2/3 一致采用多数）；3 方分歧 → 合并建议 + 标记 low_consensus
 */
@Slf4j
@Service
public class AgentVotingServiceImpl implements AgentVotingService {

    private static final long VOTE_TIMEOUT_SECONDS = 45;

    private static final Map<String, String> VOTING_PROMPTS = Map.of(
            "DataDriven", "你是数据驱动分析师Agent。分析商品数据时优先考虑 GMV 和转化率指标。"
                    + "推荐最能提升直播间销售额的商品组合。返回JSON格式：{\"recommendation\":{\"mainProduct\":{\"name\":\"\",\"reason\":\"\"},\"trafficProducts\":[],\"profitProducts\":[]},\"reasoning\":\"\",\"perspective\":\"data_driven\"}",
            "UserExperience", "你是用户体验分析师Agent。分析商品数据时优先考虑互动率和弹幕情绪。"
                    + "推荐最能提升观众互动和留存的商品组合。返回JSON格式：{\"recommendation\":{\"mainProduct\":{\"name\":\"\",\"reason\":\"\"},\"trafficProducts\":[],\"profitProducts\":[]},\"reasoning\":\"\",\"perspective\":\"user_experience\"}",
            "BrandStrategy", "你是品牌策略分析师Agent。分析商品数据时优先考虑复购率和长期品牌价值。"
                    + "推荐最能提升品牌心智和长期客户价值的商品组合。返回JSON格式：{\"recommendation\":{\"mainProduct\":{\"name\":\"\",\"reason\":\"\"},\"trafficProducts\":[],\"profitProducts\":[]},\"reasoning\":\"\",\"perspective\":\"brand_strategy\"}"
    );

    @Autowired(required = false)
    private LlmClient llmClient;

    @Override
    public String voteOnProductAnalysis(String userPrompt, AiModel model) {
        if (llmClient == null) {
            return "{\"error\":\"LLM服务不可用\",\"votingDetails\":{\"status\":\"unavailable\"}}";
        }

        // 并行启动 3 个投票 Agent
        Map<String, CompletableFuture<String>> voteFutures = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : VOTING_PROMPTS.entrySet()) {
            String agentName = entry.getKey();
            String systemPrompt = entry.getValue();
            voteFutures.put(agentName, CompletableFuture.supplyAsync(() -> {
                try {
                    var resp = llmClient.chat(model, systemPrompt, userPrompt);
                    return resp != null && resp.success() ? resp.content() : null;
                } catch (Exception e) {
                    log.warn("[AgentVoting] {} 投票失败: {}", agentName, e.getMessage());
                    return null;
                }
            }));
        }

        // 等待所有投票完成
        Map<String, String> voteResults = new LinkedHashMap<>();
        for (Map.Entry<String, CompletableFuture<String>> entry : voteFutures.entrySet()) {
            try {
                String result = entry.getValue().get(VOTE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (result != null) {
                    voteResults.put(entry.getKey(), result);
                }
            } catch (Exception e) {
                log.warn("[AgentVoting] {} 投票超时: {}", entry.getKey(), e.getMessage());
            }
        }

        if (voteResults.isEmpty()) {
            return "{\"error\":\"所有投票Agent均失败\",\"votingDetails\":{\"status\":\"all_failed\"}}";
        }

        return buildConsensus(voteResults);
    }

    /**
     * 构建共识结果
     * <p>
     * 策略: 简单多数 — 提取每个 Agent 的 mainProduct.name，
     * 2/3 一致 → 采用多数选择；3 方分歧 → 合并所有建议
     */
    private String buildConsensus(Map<String, String> voteResults) {
        // 提取每个 Agent 推荐的主推品名称
        Map<String, List<String>> productVotes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : voteResults.entrySet()) {
            String mainProduct = extractMainProductName(entry.getValue());
            productVotes.computeIfAbsent(mainProduct, k -> new ArrayList<>()).add(entry.getKey());
        }

        // 找出票数最多的
        String consensusProduct = null;
        int maxVotes = 0;
        for (Map.Entry<String, List<String>> entry : productVotes.entrySet()) {
            if (entry.getValue().size() > maxVotes) {
                maxVotes = entry.getValue().size();
                consensusProduct = entry.getKey();
            }
        }

        boolean hasConsensus = maxVotes >= 2;
        String consensusStatus = hasConsensus ? "majority" : "low_consensus";

        // 构建投票详情 JSON
        StringBuilder votingDetails = new StringBuilder();
        votingDetails.append("{\"status\":\"").append(consensusStatus).append("\"");
        votingDetails.append(",\"consensusProduct\":\"").append(consensusProduct != null ? consensusProduct : "").append("\"");
        votingDetails.append(",\"votes\":{");
        int idx = 0;
        for (Map.Entry<String, String> entry : voteResults.entrySet()) {
            if (idx++ > 0) votingDetails.append(",");
            String mainProd = extractMainProductName(entry.getValue());
            votingDetails.append("\"").append(entry.getKey()).append("\":\"").append(mainProd).append("\"");
        }
        votingDetails.append("}}");

        // 选择投票共识最高的结果作为最终输出
        if (hasConsensus && consensusProduct != null) {
            // 找到投给共识产品的第一个 Agent 的完整输出
            for (Map.Entry<String, String> entry : voteResults.entrySet()) {
                String mainProd = extractMainProductName(entry.getValue());
                if (Objects.equals(mainProd, consensusProduct)) {
                    return entry.getValue().replaceFirst("\\}\\s*$", "") +
                            ",\"votingDetails\":" + votingDetails + "}";
                }
            }
        }

        // 无共识 → 合并所有建议
        StringBuilder merged = new StringBuilder("{\"recommendation\":{\"note\":\"多Agent分歧，合并建议\",\"perspectives\":[");
        int i = 0;
        for (Map.Entry<String, String> entry : voteResults.entrySet()) {
            if (i++ > 0) merged.append(",");
            merged.append("{\"agent\":\"").append(entry.getKey())
                    .append("\",\"analysis\":").append(entry.getValue()).append("}");
        }
        merged.append("]},\"votingDetails\":").append(votingDetails).append("}");
        return merged.toString();
    }

    private String extractMainProductName(String agentOutput) {
        if (agentOutput == null) return "unknown";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var tree = mapper.readTree(agentOutput);
            var name = tree.path("recommendation").path("mainProduct").path("name");
            if (!name.isMissingNode() && name.isTextual()) {
                return name.asText();
            }
        } catch (Exception e) {
            log.debug("[AgentVoting] JSON解析失败，返回unknown: {}", e.getMessage());
        }
        return "unknown";
    }
}
