package cn.gaifan.douyinOperations.contract.ai;

/**
 * 知识库搜索 SPI — live/content ↔ intelligence 去耦
 */
public interface KbSearchPort {

    /** RAG 查询 */
    String search(String query, String kbName);

    /** 批量相似文档搜索 */
    java.util.List<String> findSimilar(String text, int limit);
}
