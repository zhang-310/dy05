package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 时效性闭环：过期文档更新服务
 */
public interface ExpiryUpdateService {

    /**
     * 将已标记过期的文档入队，等待 LLM 更新后重新入库
     * @param maxDocs 本轮最多入队数量
     * @return 入队数量
     */
    int enqueueExpiredForUpdate(int maxDocs);
}
