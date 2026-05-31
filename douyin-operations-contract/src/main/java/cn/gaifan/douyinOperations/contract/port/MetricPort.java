package cn.gaifan.douyinOperations.contract.port;

/**
 * 指标采集 Port — 跨模块数据指标
 */
public interface MetricPort {
    record MetricPoint(String name, double value, String dimension, long timestamp) {}

    void record(String metricName, double value, String dimension);
    java.util.List<MetricPoint> query(String metricName, String tenantId, int minutes);
}
