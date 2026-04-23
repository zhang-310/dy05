package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoDataRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptEffectivenessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SvScriptEffectivenessServiceImpl implements SvScriptEffectivenessService {

    private static final Logger log = LoggerFactory.getLogger(SvScriptEffectivenessServiceImpl.class);

    @Autowired
    private SvScriptRepository scriptRepository;
    @Autowired
    private SvProjectRepository projectRepository;
    @Autowired
    private SvVideoDataRepository videoDataRepository;

    @Override
    public List<Map<String, Object>> aggregateByScriptType(Long ownerId) {
        List<SvProject> projects = projectRepository.findByOwnerIdAndDeleted(ownerId, 0);
        Map<Long, SvProject> projectByScriptId = projects.stream()
                .filter(p -> p.getScriptId() != null)
                .collect(Collectors.toMap(SvProject::getScriptId, p -> p, (a, b) -> a));

        if (projectByScriptId.isEmpty()) return List.of();

        List<SvScript> scripts = scriptRepository.findAllById(projectByScriptId.keySet());
        Map<String, List<SvScript>> byType = scripts.stream()
                .collect(Collectors.groupingBy(s -> s.getScriptType() != null ? s.getScriptType() : "unknown"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<SvScript>> entry : byType.entrySet()) {
            long totalViews = 0, totalLikes = 0, totalComments = 0;
            int videoCount = 0;

            for (SvScript script : entry.getValue()) {
                SvProject project = projectByScriptId.get(script.getId());
                if (project == null) continue;
                List<SvVideoData> dataList = videoDataRepository.findByVideoIdOrderBySnapshotDateDesc(project.getId());
                if (!dataList.isEmpty()) {
                    SvVideoData latest = dataList.get(0);
                    totalViews += latest.getViewCount() != null ? latest.getViewCount() : 0;
                    totalLikes += latest.getLikeCount() != null ? latest.getLikeCount() : 0;
                    totalComments += latest.getCommentCount() != null ? latest.getCommentCount() : 0;
                    videoCount++;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scriptType", entry.getKey());
            row.put("scriptCount", entry.getValue().size());
            row.put("videoCount", videoCount);
            row.put("totalViews", totalViews);
            row.put("totalLikes", totalLikes);
            row.put("totalComments", totalComments);
            row.put("avgViews", videoCount > 0 ? totalViews / videoCount : 0);
            result.add(row);
        }
        result.sort((a, b) -> Long.compare((long) b.get("totalViews"), (long) a.get("totalViews")));
        return result;
    }

    @Override
    public List<Map<String, Object>> aggregateByStyle(Long ownerId) {
        List<SvProject> projects = projectRepository.findByOwnerIdAndDeleted(ownerId, 0);
        Map<Long, SvProject> projectByScriptId = projects.stream()
                .filter(p -> p.getScriptId() != null)
                .collect(Collectors.toMap(SvProject::getScriptId, p -> p, (a, b) -> a));

        if (projectByScriptId.isEmpty()) return List.of();

        List<SvScript> scripts = scriptRepository.findAllById(projectByScriptId.keySet());
        Map<String, List<SvScript>> byStyle = scripts.stream()
                .collect(Collectors.groupingBy(s -> s.getStyle() != null ? s.getStyle() : "default"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<SvScript>> entry : byStyle.entrySet()) {
            long totalViews = 0, totalLikes = 0;
            int videoCount = 0;
            for (SvScript script : entry.getValue()) {
                SvProject project = projectByScriptId.get(script.getId());
                if (project == null) continue;
                List<SvVideoData> dataList = videoDataRepository.findByVideoIdOrderBySnapshotDateDesc(project.getId());
                if (!dataList.isEmpty()) {
                    SvVideoData latest = dataList.get(0);
                    totalViews += latest.getViewCount() != null ? latest.getViewCount() : 0;
                    totalLikes += latest.getLikeCount() != null ? latest.getLikeCount() : 0;
                    videoCount++;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("style", entry.getKey());
            row.put("scriptCount", entry.getValue().size());
            row.put("totalViews", totalViews);
            row.put("totalLikes", totalLikes);
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getScriptEffectiveness(Long scriptId, Long ownerId) {
        Optional<SvScript> optScript = scriptRepository.findById(scriptId);
        if (optScript.isEmpty() || !optScript.get().getOwnerId().equals(ownerId)) {
            return Map.of("error", "脚本不存在或无权限");
        }
        SvScript script = optScript.get();

        List<SvProject> allProjects = projectRepository.findByOwnerIdAndDeleted(ownerId, 0);
        SvProject project = allProjects.stream()
                .filter(p -> scriptId.equals(p.getScriptId()))
                .findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scriptId", script.getId());
        result.put("scriptType", script.getScriptType());
        result.put("style", script.getStyle());
        result.put("title", script.getTitle());

        if (project == null) {
            result.put("hasVideo", false);
            return result;
        }

        result.put("projectId", project.getId());
        result.put("projectStatus", project.getStatus());

        List<SvVideoData> dataList = videoDataRepository.findByVideoIdOrderBySnapshotDateDesc(project.getId());
        if (dataList.isEmpty()) {
            result.put("hasVideo", false);
            return result;
        }

        result.put("hasVideo", true);
        SvVideoData latest = dataList.get(0);
        result.put("latestViews", latest.getViewCount());
        result.put("latestLikes", latest.getLikeCount());
        result.put("latestComments", latest.getCommentCount());
        result.put("latestShares", latest.getShareCount());
        result.put("snapshotDate", latest.getSnapshotDate());
        result.put("dataPoints", dataList.size());

        if (dataList.size() >= 2) {
            SvVideoData first = dataList.get(dataList.size() - 1);
            long viewGrowth = (latest.getViewCount() != null ? latest.getViewCount() : 0)
                    - (first.getViewCount() != null ? first.getViewCount() : 0);
            result.put("viewGrowth", viewGrowth);
        }
        return result;
    }
}
