package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 深度进化 Agent：对低分主题/待深化问题做多轮深化，提升知识质量
 */
public interface DeepEvolveService {

    /**
     * 执行一轮深度进化（处理待深化队列中的高优先级问题）
     *
     * @param kbId     目标知识库
     * @param maxCount 本轮最多处理数量
     * @return 成功深化并入库数量
     */
    int runDeepEvolve(Long kbId, int maxCount);
}
