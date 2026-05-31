package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;

/**
 * 索引队列消费者：将 pending 任务（进化报告/爆款拆解/直播复盘）入库到知识库
 */
public interface IndexQueueConsumerService {

    /**
     * 消费一轮待处理任务
     *
     * @param batchSize 每轮最多处理数量
     * @return 成功入库数量
     */
    int consume(int batchSize);

    /**
     * 在独立事务中处理单条任务（供 consume 通过代理调用，确保 REQUIRES_NEW 生效，避免异常后 Session 残留未持久化实体）
     *
     * @param task 队列任务
     */
    void processOneTask(AiIndexQueue task);
}
