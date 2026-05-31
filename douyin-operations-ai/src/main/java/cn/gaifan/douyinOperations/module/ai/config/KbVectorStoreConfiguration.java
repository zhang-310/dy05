package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.ai.service.impl.PgVectorStoreService;
import cn.gaifan.douyinOperations.module.ai.service.impl.VectorServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 知识库向量存储：{@code app.ai.kb.store=pgvector|milvus}。
 */
@Configuration
public class KbVectorStoreConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.kb.store", havingValue = "pgvector")
    public VectorService pgVectorStoreService(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            @Qualifier("milvusVectorService") VectorServiceImpl milvusVectorService
    ) {
        return new PgVectorStoreService(jdbcTemplate, milvusVectorService);
    }
}
