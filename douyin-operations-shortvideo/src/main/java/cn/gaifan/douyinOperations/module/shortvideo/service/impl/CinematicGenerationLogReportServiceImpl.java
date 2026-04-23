package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.CinematicGenerationLogReportService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CinematicGenerationLogReportServiceImpl implements CinematicGenerationLogReportService {

    @Resource
    private SvGenerationLogRepository generationLogRepository;

    @Override
    public Map<String, Object> summarize(Long ownerId, long startMs, long endMs) {
        Timestamp start = new Timestamp(startMs);
        Timestamp end = new Timestamp(endMs);
        List<SvGenerationLog> logs = ownerId != null
                ? generationLogRepository.findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(ownerId, start, end)
                : generationLogRepository.findByCreateTimeBetweenOrderByCreateTimeAsc(start, end);
        int n = logs.size();
        long ok = logs.stream().filter(l -> Boolean.TRUE.equals(l.getSuccess())).count();
        double successRate = n > 0 ? Math.round(ok * 1000.0 / n) / 10.0 : 0;
        List<Long> ms = logs.stream()
                .map(SvGenerationLog::getGenerationTimeMs)
                .filter(m -> m != null && m > 0)
                .toList();
        double avgMs = ms.isEmpty() ? 0 : Math.round(ms.stream().mapToLong(Long::longValue).average().orElse(0) * 10) / 10.0;

        Map<String, Long> byCamera = logs.stream()
                .filter(l -> l.getCameraType() != null && !l.getCameraType().isBlank())
                .collect(Collectors.groupingBy(SvGenerationLog::getCameraType, Collectors.counting()));
        Map<String, Long> byProvider = logs.stream()
                .filter(l -> l.getAiProvider() != null && !l.getAiProvider().isBlank())
                .collect(Collectors.groupingBy(SvGenerationLog::getAiProvider, Collectors.counting()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", n);
        out.put("successCount", ok);
        out.put("successRatePercent", successRate);
        out.put("avgGenerationTimeMs", avgMs);
        out.put("byCameraType", new LinkedHashMap<>(byCamera));
        out.put("byProvider", new LinkedHashMap<>(byProvider));
        return out;
    }
}
