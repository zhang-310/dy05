package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

public interface CompetitorMonitorService {
    void addCompetitor(Long ownerId, String accountId, String accountName, String platform);
    List<Map<String, Object>> listCompetitors(Long ownerId);
    void removeCompetitor(Long ownerId, Long competitorId);
    Map<String, Object> analyzeCompetitor(Long ownerId, Long competitorId);
    String generateWeeklyReport(Long ownerId);
}
