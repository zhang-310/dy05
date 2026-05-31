package cn.gaifan.douyinOperations.contract.ai;

/**
 * AI 生成 SPI — live ↔ intelligence 去耦接口
 *
 * Vision: 消除 170 imports 的直接依赖，改为 contract port 收口
 */
public interface AiGenerationPort {

    /** 生成直播话术 */
    String generateLiveScript(String productInfo, String audienceProfile, String style);

    /** 生成短视频文案 */
    String generateShortVideoCopy(String productName, String platform, String tone);

    /** 批量生成话术 */
    java.util.List<String> batchGenerateScripts(java.util.List<String> prompts);
}
