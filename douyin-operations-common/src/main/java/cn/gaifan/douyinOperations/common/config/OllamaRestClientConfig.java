package cn.gaifan.douyinOperations.common.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Ollama / Spring AI RestClient 超时配置。
 * 默认读超时过短会导致长文本生成时 SocketTimeoutException，参考 spring-projects/spring-ai#1634。
 */
@Configuration
public class OllamaRestClientConfig {

    /**
     * 延长 RestClient 读超时至 5 分钟，适配 Ollama/LLM 长生成场景（话术生成、成篇优化等）。
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);   // 30 秒连接超时
            factory.setReadTimeout(300000);    // 5 分钟读超时，适配 Ollama/LLM 长生成（话术、成篇优化）
            restClientBuilder.requestFactory(factory);
        };
    }
}
