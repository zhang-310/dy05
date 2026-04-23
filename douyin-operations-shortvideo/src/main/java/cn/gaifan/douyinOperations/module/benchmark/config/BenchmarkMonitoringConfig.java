package cn.gaifan.douyinOperations.module.benchmark.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 对标账号分析系统监控配置
 *
 * 监控指标：
 * - 自动入库成功率
 * - 向量生成耗时
 * - 相似度查询耗时
 * - 推荐查询耗时
 * - Milvus 操作耗时
 */
@Configuration
public class BenchmarkMonitoringConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> autoIngestSuccessRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> similarityCacheHitRate = new AtomicReference<>(0.0);
    private final AtomicReference<Double> recommendationAvgResultCount = new AtomicReference<>(0.0);

    public BenchmarkMonitoringConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        // 自动入库指标
        meterRegistry.counter("benchmark.auto_ingest.total", "module", "benchmark");
        meterRegistry.counter("benchmark.auto_ingest.success", "module", "benchmark");
        meterRegistry.counter("benchmark.auto_ingest.failed", "module", "benchmark");
        meterRegistry.gauge("benchmark.auto_ingest.success_rate", autoIngestSuccessRate, AtomicReference::get);

        // 向量生成指标
        meterRegistry.counter("benchmark.embedding.generated", "module", "benchmark");
        meterRegistry.counter("benchmark.embedding.failed", "module", "benchmark");
        meterRegistry.timer("benchmark.embedding.duration", "module", "benchmark");

        // 相似度查询指标
        meterRegistry.counter("benchmark.similarity.queries", "module", "benchmark");
        meterRegistry.timer("benchmark.similarity.query_duration", "module", "benchmark");
        meterRegistry.gauge("benchmark.similarity.cache_hit_rate", similarityCacheHitRate, AtomicReference::get);

        // 推荐查询指标
        meterRegistry.counter("benchmark.recommendation.queries", "module", "benchmark");
        meterRegistry.timer("benchmark.recommendation.query_duration", "module", "benchmark");
        meterRegistry.gauge("benchmark.recommendation.avg_result_count", recommendationAvgResultCount, AtomicReference::get);

        // Milvus 操作指标
        meterRegistry.counter("benchmark.milvus.operations", "module", "benchmark");
        meterRegistry.counter("benchmark.milvus.failures", "module", "benchmark");
        meterRegistry.timer("benchmark.milvus.operation_duration", "module", "benchmark");

        // 数据库操作指标
        meterRegistry.counter("benchmark.database.queries", "module", "benchmark");
        meterRegistry.timer("benchmark.database.query_duration", "module", "benchmark");
    }

    /**
     * 记录自动入库操作
     */
    public void recordAutoIngest(boolean success, long durationMs) {
        meterRegistry.counter("benchmark.auto_ingest.total").increment();
        if (success) {
            meterRegistry.counter("benchmark.auto_ingest.success").increment();
        } else {
            meterRegistry.counter("benchmark.auto_ingest.failed").increment();
        }
        meterRegistry.timer("benchmark.auto_ingest.duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录向量生成操作
     */
    public void recordEmbeddingGeneration(boolean success, long durationMs) {
        if (success) {
            meterRegistry.counter("benchmark.embedding.generated").increment();
        } else {
            meterRegistry.counter("benchmark.embedding.failed").increment();
        }
        meterRegistry.timer("benchmark.embedding.duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录相似度查询
     */
    public void recordSimilarityQuery(long durationMs, int resultCount) {
        meterRegistry.counter("benchmark.similarity.queries").increment();
        meterRegistry.timer("benchmark.similarity.query_duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录推荐查询
     */
    public void recordRecommendationQuery(long durationMs, int resultCount) {
        meterRegistry.counter("benchmark.recommendation.queries").increment();
        meterRegistry.timer("benchmark.recommendation.query_duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 Milvus 操作
     */
    public void recordMilvusOperation(boolean success, long durationMs) {
        meterRegistry.counter("benchmark.milvus.operations").increment();
        if (!success) {
            meterRegistry.counter("benchmark.milvus.failures").increment();
        }
        meterRegistry.timer("benchmark.milvus.operation_duration").record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
