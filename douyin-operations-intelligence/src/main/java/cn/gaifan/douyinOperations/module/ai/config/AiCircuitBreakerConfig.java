package cn.gaifan.douyinOperations.module.ai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * AI 混合检索 + LLM 调用熔断器：Milvus、Elasticsearch、LLM Provider 独立熔断，参数可配置
 */
@Configuration
public class AiCircuitBreakerConfig {

    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${app.ai.hybrid.circuit-breaker.milvus-failure-threshold:50}") float milvusFailureThreshold,
            @Value("${app.ai.hybrid.circuit-breaker.milvus-wait-duration-open:30s}") Duration milvusWaitOpen,
            @Value("${app.ai.hybrid.circuit-breaker.es-failure-threshold:50}") float esFailureThreshold,
            @Value("${app.ai.hybrid.circuit-breaker.es-wait-duration-open:30s}") Duration esWaitOpen,
            @Value("${app.ai.hybrid.circuit-breaker.llm-failure-threshold:40}") float llmFailureThreshold,
            @Value("${app.ai.hybrid.circuit-breaker.llm-wait-duration-open:120s}") Duration llmWaitOpen,
            @Value("${app.ai.hybrid.circuit-breaker.llm-sliding-window-size:20}") int llmSlidingWindowSize,
            @Value("${app.ai.hybrid.circuit-breaker.llm-half-open-calls:5}") int llmHalfOpenCalls
    ) {
        CircuitBreakerConfig milvusConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(milvusFailureThreshold)
                .waitDurationInOpenState(milvusWaitOpen)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        CircuitBreakerConfig esConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(esFailureThreshold)
                .waitDurationInOpenState(esWaitOpen)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        CircuitBreakerConfig llmConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(llmFailureThreshold)
                .waitDurationInOpenState(llmWaitOpen)
                .slidingWindowSize(llmSlidingWindowSize)
                .permittedNumberOfCallsInHalfOpenState(llmHalfOpenCalls)
                .build();
        CircuitBreakerConfig apiCircuitConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("milvus", milvusConfig);
        registry.circuitBreaker("elasticsearch", esConfig);
        registry.circuitBreaker("llmProvider", llmConfig);
        registry.circuitBreaker("api-circuit-breaker", apiCircuitConfig);
        return registry;
    }
}
