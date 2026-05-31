package cn.gaifan.douyinOperations.module.ai.search;

/**
 * S-4：检索查询意图（how-to / 定义 / 对比），用于混合检索 RRF 权重与 HyDE 强度分支。
 */
public enum QueryIntent {
    /** 未命中规则或显式通用 */
    GENERAL,
    /** 操作/步骤类（如何、怎么做） */
    HOW_TO,
    /** 概念定义类（是什么、含义） */
    DEFINITION,
    /** 对比/二选一（vs、区别） */
    COMPARE
}
