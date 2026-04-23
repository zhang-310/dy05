package cn.gaifan.douyinOperations.module.ai.service;

/**
 * AI 推理审计（可选启用，用于数据回流与质量统计）
 */
public interface AiInferenceAuditService {

    void record(String capability, String provider, String modelRef, int inputChars, boolean ok, long latencyMs, String errorMessage, String metaJson);
}
