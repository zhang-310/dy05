package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCinematicPreset;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSceneCameraMapping;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCinematicPresetRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvSceneCameraMappingRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoDataRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishFeedbackService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 发布后数据反馈闭环实现 (Phase 6.2)
 * 对接抖音开放平台视频数据 API，计算内容效果评分，反馈到知识库
 */
@Service
public class PublishFeedbackServiceImpl implements PublishFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(PublishFeedbackServiceImpl.class);

    @Resource
    private SvVideoRepository videoRepository;
    @Resource
    private SvVideoDataRepository videoDataRepository;
    @Resource
    private SvCinematicPresetRepository cinematicPresetRepository;
    @Resource
    private SvSceneCameraMappingRepository sceneCameraMappingRepository;
    @Autowired(required = false)
    private DouyinApiClient douyinApiClient;
    @Autowired(required = false)
    private OAuthTokenService oauthTokenService;
    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Override
    public ContentScore analyzePerformance(Long videoId, List<Long> visibleOwnerIds) {
        if (videoId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "videoId 不能为空");
        SvVideo video = videoRepository.findByIdAndDeleted(videoId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(video.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限分析该视频");

        long views = video.getViewCount() != null ? video.getViewCount() : 0;
        int likes = video.getLikeCount() != null ? video.getLikeCount() : 0;
        int comments = video.getCommentCount() != null ? video.getCommentCount() : 0;
        int shares = video.getShareCount() != null ? video.getShareCount() : 0;

        // 尝试从抖音 API 拉取最新数据
        if (douyinApiClient != null && oauthTokenService != null && StringUtils.hasText(video.getDouyinVideoId())) {
            String accessToken = oauthTokenService.getValidAccessToken(video.getOwnerId(), "douyin");
            if (accessToken != null) {
                DouyinApiClient.VideoDataResponse apiData = douyinApiClient.getVideoData(video.getDouyinVideoId(), accessToken);
                if (apiData != null) {
                    views = apiData.totalPlay();
                    likes = (int) apiData.totalLike();
                    comments = (int) apiData.totalComment();
                    shares = (int) apiData.totalShare();
                    // 更新本地并写入快照
                    updateVideoAndSnapshot(video, apiData);
                }
            }
        }

        // 计算评分（满分 100）
        double completionRate = video.getDuration() != null && video.getDuration() > 0 ? 100.0 : 50.0;
        double engagementRate = computeEngagementRate(views, likes, comments, shares);
        double followerGrowth = 0; // 需粉丝增长数据
        double viewsVsAvg = computeViewsVsAvg(video.getAccountId(), views);
        double durationFit = 100.0; // 默认
        String performance = classifyPerformance(engagementRate, viewsVsAvg);
        List<String> insights = buildInsights(views, likes, comments, shares, performance);

        return new ContentScore(
                (completionRate + engagementRate + viewsVsAvg + durationFit) / 4,
                completionRate, engagementRate, followerGrowth, viewsVsAvg, durationFit,
                performance, insights
        );
    }

    @Override
    public Map<String, Object> generateWeeklyReport(Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Timestamp weekStartTs = Timestamp.valueOf(LocalDate.now().minusDays(7).atStartOfDay());
        List<SvVideo> videos = videoRepository.findByOwnerIdAndPublishTimeAfterAndDeleted(userId, weekStartTs, 0);

        int publishCount = videos.size();
        double avgScore = 0;
        Long bestVideoId = null;
        double bestScore = 0;
        for (SvVideo v : videos) {
            ContentScore cs = analyzePerformance(v.getId(), java.util.Collections.singletonList(userId));
            avgScore += cs.overallScore();
            if (cs.overallScore() > bestScore) {
                bestScore = cs.overallScore();
                bestVideoId = v.getId();
            }
        }
        if (publishCount > 0) avgScore /= publishCount;

        List<String> insights = new ArrayList<>();
        insights.add("本周发布 " + publishCount + " 条视频");
        if (bestVideoId != null) insights.add("表现最佳视频 ID: " + bestVideoId);

        return Map.of(
                "weekStart", LocalDate.now().minusDays(7).toString(),
                "publishCount", publishCount,
                "avgScore", BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                "bestVideoId", bestVideoId != null ? bestVideoId : "",
                "insights", insights
        );
    }

    @Override
    public Map<String, Object> generateReflectionReport(Long videoId, List<Long> visibleOwnerIds) {
        if (videoId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "videoId 不能为空");
        ContentScore score = analyzePerformance(videoId, visibleOwnerIds);
        List<String> suggestions = new ArrayList<>(score.insights());
        if (score.overallScore() < 60) {
            suggestions.add("建议优化标题和封面以提高点击率");
            suggestions.add("可尝试增加互动引导（点赞、评论、转发）");
        }
        return Map.of("videoId", videoId, "score", score.overallScore(), "suggestions", suggestions);
    }

    @Override
    public void feedbackToKnowledge(Long videoId, ContentScore score) {
        if (videoId == null || score == null) return;
        try {
            SvVideo video = videoRepository.findByIdAndDeleted(videoId, 0).orElse(null);
            if (video == null) return;

            BigDecimal successRate = BigDecimal.valueOf(Math.min(100, Math.max(0, score.overallScore())));
            BigDecimal avgScore = BigDecimal.valueOf(score.overallScore());

            // 更新 sv_cinematic_preset：若有预设 ID 关联则更新（此处简化：按 camera_type 更新通用预设）
            List<SvCinematicPreset> presets = cinematicPresetRepository.findByOwnerIdAndDeletedOrderBySuccessRateDesc(video.getOwnerId(), 0);
            if (!presets.isEmpty()) {
                SvCinematicPreset p = presets.get(0);
                cinematicPresetRepository.updateSuccessStats(p.getId(), successRate, avgScore);
            }

            // 更新 sv_scene_camera_mapping 置信度：表现好时提升首条高置信度映射
            List<SvSceneCameraMapping> mappings = sceneCameraMappingRepository.findAllByOrderByConfidenceDesc();
            if (!mappings.isEmpty() && score.overallScore() >= 60) {
                SvSceneCameraMapping m = mappings.get(0);
                BigDecimal conf = m.getConfidence() != null ? m.getConfidence() : BigDecimal.valueOf(0.5);
                BigDecimal newConf = conf.add(BigDecimal.valueOf(0.02)).min(BigDecimal.ONE);
                m.setConfidence(newConf);
                sceneCameraMappingRepository.save(m);
            }
            writePerformanceReflection(video, score);
        } catch (Exception e) {
            log.warn("feedbackToKnowledge 失败: videoId={}", videoId, e);
        }
    }

    private void writePerformanceReflection(SvVideo video, ContentScore score) {
        if (operationalStrategyKnowledgeService == null || video == null || video.getOwnerId() == null || score == null) {
            return;
        }
        String verdict = score.overallScore() >= 70 ? "成功模板" : "失败原因";
        StringBuilder sb = new StringBuilder();
        sb.append("# 发布复盘：").append(verdict).append(" - ")
                .append(StringUtils.hasText(video.getTitle()) ? video.getTitle() : "视频" + video.getId()).append("\n\n");
        sb.append("## 基础数据\n");
        sb.append("- 视频ID：").append(video.getId()).append("\n");
        sb.append("- 标题：").append(video.getTitle() != null ? video.getTitle() : "").append("\n");
        sb.append("- 描述：").append(video.getDescription() != null ? video.getDescription() : "").append("\n");
        sb.append("- 标签：").append(video.getTags() != null ? video.getTags() : "").append("\n");
        sb.append("- 时长：").append(video.getDuration() != null ? video.getDuration() : 0).append("秒\n");
        sb.append("- 播放：").append(video.getViewCount() != null ? video.getViewCount() : 0).append("\n");
        sb.append("- 点赞：").append(video.getLikeCount() != null ? video.getLikeCount() : 0).append("\n");
        sb.append("- 评论：").append(video.getCommentCount() != null ? video.getCommentCount() : 0).append("\n");
        sb.append("- 分享：").append(video.getShareCount() != null ? video.getShareCount() : 0).append("\n\n");
        sb.append("## 评分拆解\n");
        sb.append("- 综合分：").append(round(score.overallScore())).append("\n");
        sb.append("- 互动率分：").append(round(score.engagementRate())).append("\n");
        sb.append("- 播放对比均值分：").append(round(score.viewsVsAvg())).append("\n");
        sb.append("- 表现等级：").append(score.performance()).append("\n\n");
        sb.append("## 自动提炼\n");
        if (score.overallScore() >= 70) {
            sb.append("- 成功模板：保留该视频的标题承诺、前 3 秒钩子、节奏长度、互动引导和发布标签作为同品类优先模板。\n");
            sb.append("- 下一轮应用：短视频脚本和数字人成片生成时优先检索本模板，复用结构不复刻原文。\n");
        } else {
            sb.append("- 失败原因：标题封面、前三秒钩子、互动引导或内容承诺可能不足，需要结合指标重新拆解。\n");
            sb.append("- 下一轮规避：生成脚本时降低空泛口号，强化证据镜头、评论痛点和合规 CTA。\n");
        }
        for (String insight : score.insights()) {
            sb.append("- ").append(insight).append("\n");
        }
        operationalStrategyKnowledgeService.writePerformanceReflection(
                video.getOwnerId(),
                verdict + "-" + (StringUtils.hasText(video.getTitle()) ? video.getTitle() : video.getId()),
                sb.toString(),
                Map.of(
                        "source", "publish_feedback",
                        "videoId", String.valueOf(video.getId()),
                        "performance", score.performance(),
                        "templateType", verdict
                )
        );
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void updateVideoAndSnapshot(SvVideo video, DouyinApiClient.VideoDataResponse apiData) {
        video.setViewCount(apiData.totalPlay());
        video.setLikeCount((int) apiData.totalLike());
        video.setCommentCount((int) apiData.totalComment());
        video.setShareCount((int) apiData.totalShare());
        video.setLastSyncTime(new Timestamp(System.currentTimeMillis()));
        video.setSyncStatus("synced");
        videoRepository.save(video);

        Date today = Date.valueOf(LocalDate.now());
        SvVideoData snapshot = videoDataRepository.findByVideoIdAndSnapshotDate(video.getId(), today)
                .orElseGet(() -> {
                    SvVideoData s = new SvVideoData();
                    s.setVideoId(video.getId());
                    s.setSnapshotDate(today);
                    return s;
                });
        snapshot.setViewCount(apiData.totalPlay());
        snapshot.setLikeCount((int) apiData.totalLike());
        snapshot.setCommentCount((long) apiData.totalComment());
        snapshot.setShareCount((long) apiData.totalShare());
        videoDataRepository.save(snapshot);
    }

    private double computeEngagementRate(long views, int likes, int comments, int shares) {
        if (views <= 0) return 0;
        double rate = (likes + comments * 2 + shares * 3) * 100.0 / views;
        return Math.min(100, rate * 10);
    }

    private double computeViewsVsAvg(Long accountId, long views) {
        if (accountId == null) return 50;
        List<Long> counts = videoRepository.findViewCountsByAccountId(accountId);
        if (counts.isEmpty()) return 50;
        double avg = counts.stream().mapToLong(Long::longValue).average().orElse(views);
        if (avg <= 0) return 50;
        double ratio = views / avg;
        return Math.min(100, Math.max(0, 50 + (ratio - 1) * 25));
    }

    private String classifyPerformance(double engagementRate, double viewsVsAvg) {
        double avg = (engagementRate + viewsVsAvg) / 2;
        if (avg >= 80) return "优秀";
        if (avg >= 60) return "良好";
        if (avg >= 40) return "一般";
        return "待优化";
    }

    private List<String> buildInsights(long views, int likes, int comments, int shares, String performance) {
        List<String> list = new ArrayList<>();
        list.add("播放量: " + views + "，点赞: " + likes + "，评论: " + comments + "，转发: " + shares);
        list.add("综合表现: " + performance);
        return list;
    }
}
