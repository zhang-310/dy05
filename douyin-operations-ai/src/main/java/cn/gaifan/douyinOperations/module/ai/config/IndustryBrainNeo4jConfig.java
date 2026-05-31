package cn.gaifan.douyinOperations.module.ai.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

/**
 * 行业大脑 Neo4j 按需配置
 * 仅当 app.ai.brain.knowledge-graph.neo4j-uri 非空时启用
 */
@Configuration
@ConditionalOnExpression("!'${app.ai.brain.knowledge-graph.neo4j-uri:}'.trim().isEmpty()")
public class IndustryBrainNeo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(IndustryBrainNeo4jConfig.class);

    @Bean
    public Driver neo4jDriver(@Value("${app.ai.brain.knowledge-graph.neo4j-uri}") String uri) {
        log.info("[IndustryBrain] Neo4j enabled uri={}", uri != null && uri.length() > 10 ? "***" : uri);
        return GraphDatabase.driver(uri);
    }

    @Bean
    public Neo4jClient neo4jClient(Driver driver) {
        return Neo4jClient.create(driver);
    }

    @Bean
    public Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient) {
        return new Neo4jTemplate(neo4jClient);
    }
}
