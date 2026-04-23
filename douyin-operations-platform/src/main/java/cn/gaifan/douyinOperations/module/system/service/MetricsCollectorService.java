package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.module.system.vo.MetricsVO;
import java.util.List;

/**
 * 指标收集服务
 */
public interface MetricsCollectorService {

    /**
     * 收集 CPU 指标
     */
    MetricsVO collectCpuMetrics();

    /**
     * 收集内存指标
     */
    MetricsVO collectMemoryMetrics();

    /**
     * 收集磁盘指标
     */
    MetricsVO collectDiskMetrics();

    /**
     * 收集 JVM 指标
     */
    MetricsVO collectJvmMetrics();

    /**
     * 收集数据库连接池指标
     */
    MetricsVO collectDatabaseMetrics();

    /**
     * 收集所有指标
     */
    List<MetricsVO> collectAllMetrics();

    /**
     * 按指标名查询
     */
    MetricsVO getMetricsByName(String name);
}
