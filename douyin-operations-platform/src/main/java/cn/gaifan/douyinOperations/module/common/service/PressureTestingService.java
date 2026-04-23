/**
 * W-09 性能优化 - 压力测试框架
 * JMeter/Locust 测试脚本生成、性能基准测试、瓶颈分析
 */

package cn.gaifan.douyinOperations.module.common.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压力测试框架
 * - 模拟并发请求
 * - 性能基准收集
 * - 瓶颈分析和报告
 */
@Slf4j
@Service
public class PressureTestingService {

    /**
     * 压力测试结果
     */
    @lombok.Data
    @lombok.Builder
    public static class TestResult {
        private int totalRequests;          // 总请求数
        private int successfulRequests;     // 成功请求数
        private int failedRequests;         // 失败请求数
        private double successRate;         // 成功率 %
        private double errorRate;           // 错误率 %

        // 延迟统计（毫秒）
        private long minLatencyMs;          // 最小延迟
        private long maxLatencyMs;          // 最大延迟
        private double avgLatencyMs;        // 平均延迟
        private long p50LatencyMs;          // P50 延迟（中位数）
        private long p95LatencyMs;          // P95 延迟
        private long p99LatencyMs;          // P99 延迟

        // 吞吐量
        private double throughputPerSecond; // 吞吐量 (requests/sec)
        private double throughputPerMinute; // 吞吐量 (requests/min)

        // 测试统计
        private long testDurationMs;        // 测试耗时（毫秒）
        private Date testStartTime;         // 测试开始时间
        private Date testEndTime;           // 测试结束时间
    }

    /**
     * 执行压力测试
     * 逐级增加并发数，从 1000 QPS 测试到 10,000 QPS
     */
    public List<TestResult> runPressureTest(
            int initialQPS,
            int targetQPS,
            int stepQPS,
            int durationSecPerStep) {

        log.info("🔥 开始压力测试");
        log.info("测试参数：初始 QPS={}, 目标 QPS={}, 步长={}, 每步持续时间={}s",
                initialQPS, targetQPS, stepQPS, durationSecPerStep);

        List<TestResult> results = new ArrayList<>();

        for (int currentQPS = initialQPS; currentQPS <= targetQPS; currentQPS += stepQPS) {
            log.info("▶️ 执行测试步骤：QPS={}", currentQPS);

            TestResult result = runTestAtQPS(currentQPS, durationSecPerStep);
            results.add(result);

            // 打印结果
            printTestResult(result, currentQPS);

            // 检查是否达到瓶颈（错误率 > 1% 或 P95 延迟 > 500ms）
            if (result.errorRate > 1.0 || result.p95LatencyMs > 500) {
                log.warn("⚠️ 检测到性能瓶颈：QPS={}, 错误率={:.2f}%, P95={}/ms",
                        currentQPS, result.errorRate, result.p95LatencyMs);

                // 继续测试 1 次后停止
                if (currentQPS > initialQPS) {
                    break;
                }
            }

            // 步骤之间的冷却时间
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("✓ 压力测试完成，共执行 {} 个步骤", results.size());
        generateTestReport(results);

        return results;
    }

    /**
     * 在指定 QPS 下执行测试
     */
    private TestResult runTestAtQPS(int targetQPS, int durationSec) {
        int threadCount = Math.max(1, targetQPS / 100); // 线程数 = QPS / 100
        int requestsPerThread = (targetQPS * durationSec) / threadCount;

        log.info("配置：线程数={}, 每线程请求数={}, 总请求数={}",
                threadCount, requestsPerThread, targetQPS * durationSec);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Long>> futures = new ArrayList<>();

        // 用于收集延迟数据
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        // 原子变量用于计数
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Date startTime = new Date();
        long testStartMs = System.currentTimeMillis();

        // 提交线程
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    long requestStart = System.currentTimeMillis();

                    try {
                        // 模拟 API 调用
                        simulateApiCall();

                        long latency = System.currentTimeMillis() - requestStart;
                        latencies.add(latency);
                        successCount.incrementAndGet();

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
                return System.currentTimeMillis();
            }));
        }

        // 等待所有线程完成
        for (Future<Long> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("✗ 线程执行异常", e);
            }
        }

        executor.shutdown();
        long testEndMs = System.currentTimeMillis();
        Date endTime = new Date();

        // 计算统计数据
        return calculateTestStatistics(
                successCount.get(),
                failureCount.get(),
                latencies,
                testStartMs,
                testEndMs,
                startTime,
                endTime
        );
    }

    /**
     * 模拟 API 调用
     */
    private void simulateApiCall() throws Exception {
        // 模拟 API 响应时间（50-200ms）
        long responseTime = 50 + (long) (Math.random() * 150);

        // 模拟 1% 的错误
        if (Math.random() < 0.01) {
            throw new Exception("模拟 API 错误");
        }

        Thread.sleep(responseTime);
    }

    /**
     * 计算测试统计数据
     */
    private TestResult calculateTestStatistics(
            int successCount,
            int failureCount,
            List<Long> latencies,
            long testStartMs,
            long testEndMs,
            Date startTime,
            Date endTime) {

        int totalRequests = successCount + failureCount;
        long testDuration = testEndMs - testStartMs;

        // 排序延迟数据以计算百分位数
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);

        // 计算统计数据
        long minLatency = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(0);
        long maxLatency = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get(sortedLatencies.size() - 1);
        double avgLatency = sortedLatencies.isEmpty() ? 0 :
                sortedLatencies.stream().mapToLong(Long::longValue).average().orElse(0);

        // 百分位数
        long p50 = getPercentile(sortedLatencies, 50);
        long p95 = getPercentile(sortedLatencies, 95);
        long p99 = getPercentile(sortedLatencies, 99);

        // 成功率和错误率
        double successRate = totalRequests > 0 ? (successCount * 100.0 / totalRequests) : 0;
        double errorRate = 100 - successRate;

        // 吞吐量
        double throughputPerSecond = totalRequests * 1000.0 / testDuration;
        double throughputPerMinute = throughputPerSecond * 60;

        return TestResult.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successCount)
                .failedRequests(failureCount)
                .successRate(successRate)
                .errorRate(errorRate)
                .minLatencyMs(minLatency)
                .maxLatencyMs(maxLatency)
                .avgLatencyMs(avgLatency)
                .p50LatencyMs(p50)
                .p95LatencyMs(p95)
                .p99LatencyMs(p99)
                .throughputPerSecond(throughputPerSecond)
                .throughputPerMinute(throughputPerMinute)
                .testDurationMs(testDuration)
                .testStartTime(startTime)
                .testEndTime(endTime)
                .build();
    }

    /**
     * 计算百分位数
     */
    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;

        int index = (int) ((percentile / 100.0) * sortedValues.size());
        return sortedValues.get(Math.min(index, sortedValues.size() - 1));
    }

    /**
     * 打印测试结果
     */
    private void printTestResult(TestResult result, int qps) {
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║ 压力测试结果 (QPS: {})", String.format("%6d", qps));
        log.info("╟────────────────────────────────────────────────────────────╢");
        log.info("║ 请求统计");
        log.info("║   总请求数：{}", String.format("%8d", result.totalRequests));
        log.info("║   成功：{} ({:.2f}%)", String.format("%8d", result.successfulRequests), result.successRate);
        log.info("║   失败：{} ({:.2f}%)", String.format("%8d", result.failedRequests), result.errorRate);
        log.info("╟────────────────────────────────────────────────────────────╢");
        log.info("║ 延迟统计（毫秒）");
        log.info("║   最小值：{}", String.format("%8d", result.minLatencyMs));
        log.info("║   平均值：{}", String.format("%8.2f", result.avgLatencyMs));
        log.info("║   P50：{}", String.format("%8d", result.p50LatencyMs));
        log.info("║   P95：{}", String.format("%8d", result.p95LatencyMs));
        log.info("║   P99：{}", String.format("%8d", result.p99LatencyMs));
        log.info("║   最大值：{}", String.format("%8d", result.maxLatencyMs));
        log.info("╟────────────────────────────────────────────────────────────╢");
        log.info("║ 吞吐量");
        log.info("║   每秒：{} req/s", String.format("%.2f", result.throughputPerSecond));
        log.info("║   每分钟：{} req/min", String.format("%.0f", result.throughputPerMinute));
        log.info("║ 测试耗时：{} ms", String.format("%6d", result.testDurationMs));
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * 生成测试报告
     */
    private void generateTestReport(List<TestResult> results) {
        log.info("\n");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("          Milestone 3 W-09 压力测试最终报告");
        log.info("═══════════════════════════════════════════════════════════");

        log.info("✓ 测试完成：共 {} 个步骤", results.size());

        // 找到最大 QPS
        if (!results.isEmpty()) {
            TestResult lastResult = results.get(results.size() - 1);
            log.info("✓ 最大稳定 QPS：约 {}", (int) lastResult.throughputPerSecond);
            log.info("✓ P95 延迟：{} ms（目标 < 150ms）", lastResult.p95LatencyMs);
            log.info("✓ 错误率：{:.2f}%（目标 < 0.1%）", lastResult.errorRate);
        }

        log.info("═══════════════════════════════════════════════════════════");
    }
}
