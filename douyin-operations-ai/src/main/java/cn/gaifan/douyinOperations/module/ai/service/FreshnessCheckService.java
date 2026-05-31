package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 时效性检测 Agent：检测知识文档是否过期
 */
public interface FreshnessCheckService {

    /**
     * 执行一轮时效性检测（采样未检测或久未检测的文档）
     *
     * @param kbId    知识库 ID，null 表示全部
     * @param maxDocs 本轮最多检测文档数
     * @return 检测数量
     */
    int runFreshnessCheck(Long kbId, int maxDocs);
}
