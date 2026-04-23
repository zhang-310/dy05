package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentCalendarService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 内容日历 Service 实现
 */
@Service
public class ContentCalendarServiceImpl implements ContentCalendarService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private SvProjectRepository projectRepository;
    @Resource
    private SvVideoRepository videoRepository;

    @Override
    public Map<String, Object> getCalendarView(int year, int month, List<Long> visibleOwnerIds) {
        if (visibleOwnerIds != null && visibleOwnerIds.isEmpty()) {
            return Map.of("days", Map.<String, Object>of(), "year", year, "month", month);
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        Date startDate = Date.valueOf(start);
        Date endDate = Date.valueOf(end);
        Timestamp startTs = Timestamp.valueOf(start.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(end.plusDays(1).atStartOfDay());

        List<SvProject> planned;
        List<SvProject> publishedProjects;
        List<SvVideo> publishedVideos;
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            planned = projectRepository.findByOwnerIdInAndScheduleDateBetweenAndDeleted(visibleOwnerIds, startDate, endDate);
            publishedProjects = projectRepository.findByOwnerIdInAndPublishTimeBetweenAndDeleted(visibleOwnerIds, startTs, endTs);
            publishedVideos = videoRepository.findByOwnerIdInAndPublishTimeBetweenAndDeleted(visibleOwnerIds, startTs, endTs);
        } else {
            planned = projectRepository.findByScheduleDateBetweenAndDeleted(startDate, endDate);
            publishedProjects = projectRepository.findByPublishTimeBetweenAndDeleted(startTs, endTs);
            publishedVideos = videoRepository.findByPublishTimeBetweenAndDeleted(startTs, endTs);
        }

        Map<String, List<Map<String, Object>>> days = new LinkedHashMap<>();
        for (int d = 1; d <= end.getDayOfMonth(); d++) {
            String key = LocalDate.of(year, month, d).format(DATE_FMT);
            days.put(key, new ArrayList<>());
        }

        for (SvProject p : planned) {
            if (p.getScheduleDate() != null) {
                String key = p.getScheduleDate().toLocalDate().format(DATE_FMT);
                days.computeIfAbsent(key, k -> new ArrayList<>()).add(Map.<String, Object>of(
                        "type", "planned",
                        "id", p.getId(),
                        "title", p.getTitle() != null ? p.getTitle() : "",
                        "projectType", p.getProjectType() != null ? p.getProjectType() : ""
                ));
            }
        }
        for (SvProject p : publishedProjects) {
            if (p.getPublishTime() != null) {
                String key = p.getPublishTime().toLocalDateTime().toLocalDate().format(DATE_FMT);
                days.computeIfAbsent(key, k -> new ArrayList<>()).add(Map.<String, Object>of(
                        "type", "published_project",
                        "id", p.getId(),
                        "title", p.getTitle() != null ? p.getTitle() : ""
                ));
            }
        }
        for (SvVideo v : publishedVideos) {
            if (v.getPublishTime() != null) {
                String key = v.getPublishTime().toLocalDateTime().toLocalDate().format(DATE_FMT);
                days.computeIfAbsent(key, k -> new ArrayList<>()).add(Map.<String, Object>of(
                        "type", "published_video",
                        "id", v.getId(),
                        "title", v.getTitle() != null ? v.getTitle() : ""
                ));
            }
        }

        return Map.of("days", days, "year", year, "month", month);
    }

    @Override
    public Map<String, Object> getCalendarStats(int year, int month, List<Long> visibleOwnerIds) {
        if (visibleOwnerIds != null && visibleOwnerIds.isEmpty()) {
            return Map.of("plannedCount", 0, "publishedCount", 0, "completionRate", 0.0);
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        Date startDate = Date.valueOf(start);
        Date endDate = Date.valueOf(end);
        Timestamp startTs = Timestamp.valueOf(start.atStartOfDay());
        Timestamp endTs = Timestamp.valueOf(end.plusDays(1).atStartOfDay());

        List<SvProject> planned;
        List<SvProject> publishedProjects;
        List<SvVideo> publishedVideos;
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            planned = projectRepository.findByOwnerIdInAndScheduleDateBetweenAndDeleted(visibleOwnerIds, startDate, endDate);
            publishedProjects = projectRepository.findByOwnerIdInAndPublishTimeBetweenAndDeleted(visibleOwnerIds, startTs, endTs);
            publishedVideos = videoRepository.findByOwnerIdInAndPublishTimeBetweenAndDeleted(visibleOwnerIds, startTs, endTs);
        } else {
            planned = projectRepository.findByScheduleDateBetweenAndDeleted(startDate, endDate);
            publishedProjects = projectRepository.findByPublishTimeBetweenAndDeleted(startTs, endTs);
            publishedVideos = videoRepository.findByPublishTimeBetweenAndDeleted(startTs, endTs);
        }

        int plannedCount = planned.size();
        int publishedCount = publishedProjects.size() + publishedVideos.size();
        double completionRate = plannedCount > 0 ? (double) Math.min(publishedCount, plannedCount) / plannedCount * 100 : 0;

        return Map.of(
                "plannedCount", plannedCount,
                "publishedCount", publishedCount,
                "completionRate", Math.round(completionRate * 10) / 10.0
        );
    }
}
