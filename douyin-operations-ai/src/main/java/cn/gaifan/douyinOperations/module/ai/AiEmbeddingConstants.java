package cn.gaifan.douyinOperations.module.ai;

/**
 * 全系统文本向量维度固定为 1024（Milvus、话术 BYTEA、BGE-M3、方舟文本向量化等须一致）。
 * 勿在配置中改为其他值；换模型若输出非 1024 维需另选 1024 维模型或重建集合与全量重嵌入。
 */
public final class AiEmbeddingConstants {

    private AiEmbeddingConstants() {}

    public static final int VECTOR_DIMENSION = 1024;
}
