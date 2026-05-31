package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 阶段 C：自动将进化结果合并到生产知识库前的门闸（默认关闭自动合并）。
 */
public interface EvolutionInfrastructureGate {

    /** 未开启自动合并或策略不允许时抛出 FORBIDDEN */
    void assertAutoMergeAllowed();
}
