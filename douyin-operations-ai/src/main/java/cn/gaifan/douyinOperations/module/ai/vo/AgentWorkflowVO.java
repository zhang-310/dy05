package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;
import java.util.List;

@Data
public class AgentWorkflowVO {
    private List<AgentNode> nodes;
    private List<AgentEdge> edges;

    @Data
    public static class AgentNode {
        private String agentId;
        private String role;         // ProductAnalyst, ScriptWriter, ComplianceChecker, ScheduleOptimizer
        private String systemPrompt;
        private List<String> tools;
    }

    @Data
    public static class AgentEdge {
        private String from;
        private String to;
    }
}
