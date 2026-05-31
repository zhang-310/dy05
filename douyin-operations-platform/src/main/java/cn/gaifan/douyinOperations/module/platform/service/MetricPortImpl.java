package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.MetricPort;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricPortImpl implements MetricPort {

    private final Map<String, List<MetricPoint>> store = new ConcurrentHashMap<>();

    @Override
    public void record(String metricName, double value, String dimension) {
        store.computeIfAbsent(metricName, k -> new ArrayList<>())
                .add(new MetricPoint(metricName, value, dimension, System.currentTimeMillis()));
    }

    @Override
    public List<MetricPoint> query(String metricName, String tenantId, int minutes) {
        long cutoff = System.currentTimeMillis() - minutes * 60000L;
        return store.getOrDefault(metricName, List.of()).stream()
                .filter(p -> p.timestamp() > cutoff)
                .toList();
    }
}
