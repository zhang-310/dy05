package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterial;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvMaterialRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoDataRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoDashboardService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 短视频 Dashboard 服务实现
 */
@Service
public class ShortVideoDashboardServiceImpl implements ShortVideoDashboardService {

    @Resource
    private SvProjectRepository projectRepository;
    @Resource
    private SvMaterialRepository materialRepository;
    @Resource
    private SvVideoRepository videoRepository;
    @Resource
    private SvVideoDataRepository videoDataRepository;

    private static final int[] PROGRESS_STEPS = {16, 33, 50, 66, 83, 100}; // 脚本/分镜/素材准备/素材生产/剪辑/发布

    @Override
    public Map<String, Object> getStats(Long ownerId) {
        if (ownerId == null) return emptyStats();
        List<SvProject> all = projectRepository.findByOwnerIdAndDeleted(ownerId, 0);
        long totalVideoCount = all.stream().filter(p -> StringUtils.hasText(p.getFinalVideoUrl())).count();
        List<SvVideo> videos = videoRepository.findByOwnerIdAndDeleted(ownerId, 0);
        long totalPlayCount = videos.stream()
                .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L)
                .sum();
        Map<String, Object> costMap = getCostBreakdown(ownerId, null);
        double totalCost = costMap.get("total") instanceof Number ? ((Number) costMap.get("total")).doubleValue() : 0.0;
        double roi = totalPlayCount > 0 && totalCost > 0 ? (totalPlayCount / 1000.0) / totalCost : 0.0;
        Map<String, Object> m = new HashMap<>();
        m.put("totalVideoCount", totalVideoCount);
        m.put("totalPlayCount", totalPlayCount);
        m.put("totalCost", Math.round(totalCost * 100) / 100.0);
        m.put("roi", Math.round(roi * 100) / 100.0);
        m.put("totalPlayCountChange", 0);
        m.put("totalCostChange", 0.0);
        m.put("roiChange", 0.0);
        return m;
    }

    @Override
    public List<Map<String, Object>> getTrend(Long ownerId, int days) {
        if (ownerId == null) return List.of();
        List<SvVideo> videos = videoRepository.findByOwnerIdAndDeleted(ownerId, 0);
        List<Long> videoIds = videos.stream().map(SvVideo::getId).toList();
        if (videoIds.isEmpty()) {
            return buildTrendPlaceholder(days);
        }
        LocalDate today = LocalDate.now();
        Date start = Date.valueOf(today.minusDays(days - 1));
        Date end = Date.valueOf(today);
        List<SvVideoData> dataList = videoDataRepository.findByVideoIdInAndSnapshotDateBetween(videoIds, start, end);
        Map<String, long[]> byDate = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (SvVideoData d : dataList) {
            String key = d.getSnapshotDate().toLocalDate().format(fmt);
            long[] arr = byDate.computeIfAbsent(key, k -> new long[3]);
            arr[0] += (d.getViewCount() != null ? d.getViewCount() : 0L);
            arr[1] += (d.getLikeCount() != null ? d.getLikeCount() : 0);
            arr[2] += (d.getCommentCount() != null ? d.getCommentCount() : 0);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dateStr = d.format(fmt);
            Map<String, Object> m = new HashMap<>();
            m.put("date", dateStr);
            long[] arr = byDate.getOrDefault(dateStr, new long[3]);
            m.put("playCount", arr[0]);
            m.put("likeCount", arr[1]);
            m.put("commentCount", arr[2]);
            list.add(m);
        }
        return list;
    }

    private static List<Map<String, Object>> buildTrendPlaceholder(int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Map<String, Object> m = new HashMap<>();
            m.put("date", d.format(fmt));
            m.put("playCount", 0L);
            m.put("likeCount", 0L);
            m.put("commentCount", 0L);
            list.add(m);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getProjectsWithProgress(Long ownerId, String status, int page, int rows) {
        if (ownerId == null) return List.of();
        var sort = Sort.by(Sort.Direction.DESC, "createTime");
        var pageable = PageRequest.of(page, rows, sort);
        var spec = (org.springframework.data.jpa.domain.Specification<SvProject>) (root, query, cb) -> {
            var preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), ownerId));
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(status)) {
                preds.add(cb.equal(root.get("status"), status.trim()));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var pageResult = projectRepository.findAll(spec, pageable);
        List<Long> projectIds = pageResult.getContent().stream().map(SvProject::getId).toList();
        Map<Long, Long> keyframeCount = countMaterialsByProject(projectIds, "image");
        Map<Long, Long> videoCount = countMaterialsByProject(projectIds, "video");
        Map<Long, Long> audioCount = countMaterialsByProject(projectIds, "audio");

        return pageResult.getContent().stream().map(p -> toProjectWithProgress(p, keyframeCount, videoCount, audioCount)).toList();
    }

    private Map<Long, Long> countMaterialsByProject(List<Long> projectIds, String materialType) {
        if (projectIds.isEmpty()) return Map.of();
        List<SvMaterial> list = materialRepository.findByProjectIdInAndMaterialType(projectIds, materialType);
        return list.stream().collect(Collectors.groupingBy(m -> m.getProjectId() != null ? m.getProjectId() : -1L, Collectors.counting()));
    }

    private Map<String, Object> toProjectWithProgress(SvProject p, Map<Long, Long> keyframeCount, Map<Long, Long> videoCount, Map<Long, Long> audioCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("status", p.getStatus());
        m.put("projectType", p.getProjectType());
        m.put("thumbnailUrl", p.getThumbnailUrl());
        m.put("finalVideoUrl", p.getFinalVideoUrl());
        m.put("createTime", p.getCreateTime());
        m.put("updateTime", p.getUpdateTime());
        m.put("scriptId", p.getScriptId());
        m.put("shotListId", p.getShotListId());

        int progress = computeProgress(p, keyframeCount.getOrDefault(p.getId(), 0L).intValue(),
                videoCount.getOrDefault(p.getId(), 0L).intValue(), audioCount.getOrDefault(p.getId(), 0L).intValue());
        String stage = computeStage(p, keyframeCount.getOrDefault(p.getId(), 0L).intValue(),
                videoCount.getOrDefault(p.getId(), 0L).intValue(), audioCount.getOrDefault(p.getId(), 0L).intValue());

        m.put("progress", progress);
        m.put("stage", stage);
        return m;
    }

    private int computeProgress(SvProject p, int keyframeCnt, int videoCnt, int audioCnt) {
        if (StringUtils.hasText(p.getFinalVideoUrl())) return 100;
        if (keyframeCnt > 0 && videoCnt > 0 && audioCnt > 0) return 83; // 剪辑
        if (keyframeCnt > 0 || videoCnt > 0 || audioCnt > 0) return 66; // 素材生产中
        if (p.getShotListId() != null) return 50; // 分镜完成，待素材
        if (p.getScriptId() != null) return 33; // 脚本完成
        return 16; // 仅创建
    }

    private String computeStage(SvProject p, int keyframeCnt, int videoCnt, int audioCnt) {
        if (StringUtils.hasText(p.getFinalVideoUrl())) return "审核发布";
        if (keyframeCnt > 0 && videoCnt > 0 && audioCnt > 0) return "视频剪辑";
        if (keyframeCnt > 0 || videoCnt > 0 || audioCnt > 0) return "素材生产中";
        if (p.getShotListId() != null) return "素材准备";
        if (p.getScriptId() != null) return "分镜设计";
        return "脚本策划";
    }

    private static Map<String, Object> emptyStats() {
        Map<String, Object> m = new HashMap<>();
        m.put("totalVideoCount", 0L);
        m.put("totalPlayCount", 0L);
        m.put("totalCost", 0.0);
        m.put("roi", 0.0);
        m.put("totalPlayCountChange", 0);
        m.put("totalCostChange", 0.0);
        m.put("roiChange", 0.0);
        return m;
    }

    /** 成本单价（元）：脚本/次、图像/张(ComfyUI=0)、视频/段5s、配音/段、存储/条 */
    private static final double COST_SCRIPT = 0.001;
    private static final double COST_IMAGE = 0.0;
    private static final double COST_VIDEO_PER_SEG = 0.4;
    private static final double COST_VOICE_PER_SEG = 0.01;
    private static final double COST_STORAGE_PER_ITEM = 0.01;

    @Override
    public Map<String, Object> getCostBreakdown(Long ownerId, Long projectId) {
        if (ownerId == null) return emptyCostBreakdown();
        Map<String, Object> m = new HashMap<>();
        double scriptCost = 0, imageCost = 0, videoCost = 0, voiceCost = 0, storageCost = 0;
        if (projectId != null) {
            var p = projectRepository.findById(projectId);
            if (p.isEmpty() || !ownerId.equals(p.get().getOwnerId())) return emptyCostBreakdown();
            if (p.get().getScriptId() != null) scriptCost = COST_SCRIPT;
            List<SvMaterial> images = materialRepository.findByProjectIdAndMaterialType(projectId, "image");
            List<SvMaterial> videos = materialRepository.findByProjectIdAndMaterialType(projectId, "video");
            List<SvMaterial> audios = materialRepository.findByProjectIdAndMaterialType(projectId, "audio");
            int imgCnt = images.size();
            int vidCnt = videos.size();
            int audCnt = audios.size();
            imageCost = imgCnt * COST_IMAGE;
            videoCost = vidCnt * COST_VIDEO_PER_SEG;
            voiceCost = audCnt * COST_VOICE_PER_SEG;
            storageCost = (imgCnt + vidCnt + audCnt > 0 ? 1 : 0) * COST_STORAGE_PER_ITEM;
        } else {
            List<SvProject> all = projectRepository.findByOwnerIdAndDeleted(ownerId, 0);
            List<Long> ids = all.stream().map(SvProject::getId).toList();
            if (ids.isEmpty()) return emptyCostBreakdown();
            long scriptCnt = all.stream().filter(p -> p.getScriptId() != null).count();
            Map<Long, Long> imgCnt = countMaterialsByProject(ids, "image");
            Map<Long, Long> vidCnt = countMaterialsByProject(ids, "video");
            Map<Long, Long> audCnt = countMaterialsByProject(ids, "audio");
            scriptCost = scriptCnt * COST_SCRIPT;
            long totalImg = 0, totalVid = 0, totalAud = 0;
            for (Long id : ids) {
                long ic = imgCnt.getOrDefault(id, 0L);
                long vc = vidCnt.getOrDefault(id, 0L);
                long ac = audCnt.getOrDefault(id, 0L);
                totalImg += ic;
                totalVid += vc;
                totalAud += ac;
                imageCost += ic * COST_IMAGE;
                videoCost += vc * COST_VIDEO_PER_SEG;
                voiceCost += ac * COST_VOICE_PER_SEG;
            }
            storageCost = (totalImg + totalVid + totalAud > 0 ? all.size() : 0) * COST_STORAGE_PER_ITEM;
        }
        double total = scriptCost + imageCost + videoCost + voiceCost + storageCost;
        m.put("scriptCost", Math.round(scriptCost * 1000) / 1000.0);
        m.put("imageCost", Math.round(imageCost * 1000) / 1000.0);
        m.put("videoCost", Math.round(videoCost * 1000) / 1000.0);
        m.put("voiceCost", Math.round(voiceCost * 1000) / 1000.0);
        m.put("storageCost", Math.round(storageCost * 1000) / 1000.0);
        m.put("total", Math.round(total * 100) / 100.0);
        return m;
    }

    private static Map<String, Object> emptyCostBreakdown() {
        Map<String, Object> m = new HashMap<>();
        m.put("scriptCost", 0.0);
        m.put("imageCost", 0.0);
        m.put("videoCost", 0.0);
        m.put("voiceCost", 0.0);
        m.put("storageCost", 0.0);
        m.put("total", 0.0);
        return m;
    }
}
