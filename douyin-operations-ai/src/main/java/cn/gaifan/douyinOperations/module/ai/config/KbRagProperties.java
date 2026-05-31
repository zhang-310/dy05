package cn.gaifan.douyinOperations.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库 RAG 相关配置（与 application.yml app.ai.kb.rag 对齐）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai.kb.rag")
public class KbRagProperties {

    /**
     * 单次混合检索耗时超过该阈值（毫秒）时记 SLO 违规（Counter + warn 日志）；≤0 表示关闭
     */
    private long sloHybridSearchP95MsTarget = 3000;
}
