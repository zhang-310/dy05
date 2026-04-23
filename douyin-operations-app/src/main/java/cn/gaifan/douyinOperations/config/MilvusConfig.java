package cn.gaifan.douyinOperations.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);

    @Value("${app.milvus.host:localhost}")
    private String host;

    @Value("${app.milvus.port:19530}")
    private Integer port;

    /** 启动时连接 Milvus 的最大尝试次数（容器刚起时 Proxy 常短暂未就绪） */
    @Value("${app.milvus.startup.max-attempts:30}")
    private int startupMaxAttempts;

    @Value("${app.milvus.startup.retry-interval-ms:2000}")
    private long startupRetryIntervalMs;

    @Bean
    @ConditionalOnProperty(name = "app.milvus.enabled", havingValue = "true")
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= startupMaxAttempts; attempt++) {
            try {
                return new MilvusServiceClient(connectParam);
            } catch (RuntimeException e) {
                last = e;
                if (attempt < startupMaxAttempts) {
                    log.warn("Milvus 连接失败（{}/{}）: {}，{} ms 后重试",
                            attempt, startupMaxAttempts, e.getMessage(), startupRetryIntervalMs);
                    try {
                        Thread.sleep(startupRetryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("等待 Milvus 就绪时被中断", ie);
                    }
                }
            }
        }
        throw new IllegalStateException(
                "Milvus 在 " + startupMaxAttempts + " 次尝试后仍无法连接（" + host + ":" + port + "）", last);
    }
}
