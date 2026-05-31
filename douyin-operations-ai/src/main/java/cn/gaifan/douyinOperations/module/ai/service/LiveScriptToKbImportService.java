package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 话术→知识库管道：将高效 live_script 入库 huashu，供 RAG 检索。
 * 条件：effectiveness_score >= 80，source_type=live_script
 */
public interface LiveScriptToKbImportService {

    /**
     * 为指定用户导入高效话术到 huashu 知识库
     *
     * @param userId 用户 ID
     * @param maxPerRun 本次最多导入条数（防暴量）
     * @return 本次导入数量、跳过数量、错误信息
     */
    Map<String, Object> importForUser(Long userId, int maxPerRun);

    /**
     * 为所有有 huashu 的用户执行导入
     *
     * @param maxPerUser 每用户最多导入条数
     * @return 汇总统计
     */
    Map<String, Object> importForAllUsers(int maxPerUser);

    /**
     * 按指定 LiveScript ID 列表导入（用于 EvolutionRuleEngine INCLUSION_RULE 执行）
     *
     * @param userId 用户 ID
     * @param scriptIds LiveScript.id 列表
     * @return 本次导入数量、跳过数量、错误信息
     */
    Map<String, Object> importScriptsByIds(Long userId, List<Long> scriptIds);
}
