package cn.gaifan.douyinOperations.module.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * P0-7: Agent 模块线程池配置
 * 解决 SSE 流式响应每次创建新线程池导致的线程泄漏问题
 */
@Configuration
public class AgentThreadPoolConfig {

    private ExecutorService sseExecutor;
    private ExecutorService workflowExecutor;
    private ExecutorService dagExecutor;

    /**
     * SSE 流式响应专用线程池
     * - 核心线程数：10（处理常规并发）
     * - 最大线程数：30（处理峰值流量）
     * - 队列容量：100（缓冲突发请求）
     * - 拒绝策略：CallerRunsPolicy（降级到调用线程执行）
     */
    @Bean("sseExecutor")
    public ExecutorService sseExecutor() {
        this.sseExecutor = new ThreadPoolExecutor(
            10,                                      // corePoolSize
            30,                                      // maximumPoolSize
            60L,                                     // keepAliveTime
            TimeUnit.SECONDS,                        // unit
            new LinkedBlockingQueue<>(100),          // workQueue
            new ThreadPoolExecutor.CallerRunsPolicy() // handler
        );
        return this.sseExecutor;
    }

    /**
     * 工作流执行专用线程池
     * - 核心线程数：5（工作流执行频率较低）
     * - 最大线程数：20（支持并行工作流）
     * - 队列容量：50
     * - 拒绝策略：CallerRunsPolicy
     */
    @Bean("workflowExecutor")
    public ExecutorService workflowExecutor() {
        this.workflowExecutor = new ThreadPoolExecutor(
            5,                                       // corePoolSize
            20,                                      // maximumPoolSize
            60L,                                     // keepAliveTime
            TimeUnit.SECONDS,                        // unit
            new LinkedBlockingQueue<>(50),           // workQueue
            new ThreadPoolExecutor.CallerRunsPolicy() // handler
        );
        return this.workflowExecutor;
    }

    /**
     * P0-7: DAG 并行执行专用线程池
     * - 核心线程数：10（支持同层多步骤并行）
     * - 最大线程数：50（支持大规模 DAG）
     * - 队列容量：200
     * - 拒绝策略：CallerRunsPolicy
     */
    @Bean("dagExecutor")
    public ExecutorService dagExecutor() {
        this.dagExecutor = new ThreadPoolExecutor(
            10,                                      // corePoolSize
            50,                                      // maximumPoolSize
            60L,                                     // keepAliveTime
            TimeUnit.SECONDS,                        // unit
            new LinkedBlockingQueue<>(200),          // workQueue
            new ThreadPoolExecutor.CallerRunsPolicy() // handler
        );
        return this.dagExecutor;
    }

    /**
     * P0-7: 优雅关闭所有线程池
     * 应用关闭时自动调用，避免线程泄漏
     */
    @PreDestroy
    public void shutdown() {
        shutdownExecutor(sseExecutor, "sseExecutor");
        shutdownExecutor(workflowExecutor, "workflowExecutor");
        shutdownExecutor(dagExecutor, "dagExecutor");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor == null) return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("线程池 " + name + " 未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
