package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

/**
 * 单 Agent 节点执行结果 VO
 */
@Data
public class AgentNodeResult {
    /** Agent 角色名 */
    private String role;
    /** 输出内容 */
    private String output;
    /** 输入大小（字符数） */
    private int inputSize;
    /** 输出大小（字符数） */
    private int outputSize;
    /** 耗时（毫秒） */
    private long latencyMs;
    /** 执行状态: success / failed / timeout */
    private String status;
    /** 错误信息（失败时） */
    private String errorMessage;
    /** 重试次数 */
    private int retryCount;

    public static AgentNodeResult success(String role, String output, int inputSize, long latencyMs) {
        AgentNodeResult r = new AgentNodeResult();
        r.setRole(role);
        r.setOutput(output);
        r.setInputSize(inputSize);
        r.setOutputSize(output != null ? output.length() : 0);
        r.setLatencyMs(latencyMs);
        r.setStatus("success");
        return r;
    }

    public static AgentNodeResult failed(String role, String errorMessage, int inputSize, long latencyMs, int retryCount) {
        AgentNodeResult r = new AgentNodeResult();
        r.setRole(role);
        r.setInputSize(inputSize);
        r.setLatencyMs(latencyMs);
        r.setStatus("failed");
        r.setErrorMessage(errorMessage);
        r.setRetryCount(retryCount);
        return r;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
