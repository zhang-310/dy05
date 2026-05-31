package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.common.config.ShortVideoBusinessConfig;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ViralVideoCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(ViralVideoCollectorScheduler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private SvHotTopicRepository svHotTopicRepository;

    @Resource
    private SvViralVideoRepository svViralVideoRepository;

    @Resource
    private ShortVideoBusinessConfig shortVideoBusinessConfig;

    @Resource
    private ShortVideoVerticalCollectorProperties verticalCollectorProperties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DouyinVideoRepository douyinVideoRepository;

    @Value("${app.viral-collector.enabled:true}")
    private boolean collectorEnabled;

    @Scheduled(cron = "${app.viral-collector.cron:0 0 4 * * ?}")
    public void collectViralFromHotTopics() {
        if (isDisabled("热点话题爆款采集")) {
            return;
        }
        log.info("[ViralCollector] 开始从热点话题采集爆款");
        try {
            ShortVideoBusinessConfig.ViralThreshold threshold = shortVideoBusinessConfig.getViral();

            Specification<SvHotTopic> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.greaterThan(root.get("heatScore"), 80L));
                predicates.add(cb.equal(root.get("status"), "active"));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            List<SvHotTopic> hotTopics = svHotTopicRepository.findAll(spec);
            if (hotTopics.isEmpty()) {
                log.info("[ViralCollector] 无 heatScore > 80 的热点话题");
                return;
            }

            int created = 0;
            for (SvHotTopic topic : hotTopics) {
                boolean exists = svViralVideoRepository.findAll((Specification<SvViralVideo>) (root, query, cb) ->
                        cb.and(
                                cb.equal(root.get("title"), topic.getTitle()),
                                cb.equal(root.get("deleted"), 0)
                        )).stream().findFirst().isPresent();

                if (exists) {
                    continue;
                }

                SvViralVideo viral = new SvViralVideo();
                viral.setOwnerId(0L);
                viral.setTitle(topic.getTitle());
                viral.setTags(topic.getRelatedTags());
                viral.setViralScore(topic.getHeatScore() != null ? topic.getHeatScore().intValue() : 0);
                viral.setAutoCollected(true);
                viral.setCollectSource("hot_topic_scheduler");
                viral.setAnalysisResult("来源: 热点话题自动采集, 话题ID: " + topic.getId()
                        + ", 分类: " + topic.getCategory()
                        + ", 爆款阈值: 播放>" + threshold.getMinViewCount()
                        + " 点赞率>" + (threshold.getMinLikeRate() * 100) + "%"
                        + " 完播率>" + (threshold.getMinCompletionRate() * 100) + "%"
                        + " 分享率>" + (threshold.getMinShareRate() * 100) + "%");
                svViralVideoRepository.save(viral);
                created++;
            }

            log.info("[ViralCollector] 采集完成，新增 {} 条爆款记录（共扫描 {} 个热点话题），爆款阈值: 播放>{}万/点赞率>{}%/完播率>{}%/分享率>{}%",
                    created, hotTopics.size(),
                    threshold.getMinViewCount() / 10000,
                    threshold.getMinLikeRate() * 100,
                    threshold.getMinCompletionRate() * 100,
                    threshold.getMinShareRate() * 100);
        } catch (Exception e) {
            log.error("[ViralCollector] 采集失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 douyin_video 表按 ViralThreshold 筛选高播放量视频入库爆款库。
     * 每日 4:30 执行（在热点采集之后）。
     */
    @Scheduled(cron = "${app.viral-collector.video-cron:0 30 4 * * ?}")
    public void collectViralFromDouyinVideos() {
        if (isDisabled("douyin_video 爆款采集")) {
            return;
        }
        if (douyinVideoRepository == null) {
            log.debug("[ViralCollector] DouyinVideoRepository 未注入，跳过视频爆款采集");
            return;
        }
        log.info("[ViralCollector] 开始从 douyin_video 按阈值筛选爆款");
        try {
            ShortVideoBusinessConfig.ViralThreshold threshold = shortVideoBusinessConfig.getViral();

            Specification<DouyinVideo> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.greaterThanOrEqualTo(root.get("viewCount"), threshold.getMinViewCount()));
                predicates.add(cb.equal(root.get("deleted"), 0));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            List<DouyinVideo> videos = douyinVideoRepository.findAll(spec);
            int created = 0;
            for (DouyinVideo video : videos) {
                long views = video.getViewCount() != null ? video.getViewCount() : 0;
                long likes = video.getLikeCount() != null ? video.getLikeCount() : 0;
                double likeRate = views > 0 ? (double) likes / views : 0;
                if (likeRate < threshold.getMinLikeRate()) continue;

                boolean exists = svViralVideoRepository.findAll((Specification<SvViralVideo>) (root, q, cb) ->
                        cb.and(
                                cb.equal(root.get("title"), video.getTitle()),
                                cb.equal(root.get("deleted"), 0)
                        )).stream().findFirst().isPresent();
                if (exists) continue;

                SvViralVideo viral = new SvViralVideo();
                viral.setOwnerId(0L);
                viral.setTitle(video.getTitle());
                viral.setViewCount(views);
                viral.setLikeCount(likes);
                viral.setShareCount(video.getShareCount() != null ? video.getShareCount() : 0);
                viral.setViralScore((int) Math.min(100, views / 100000));
                viral.setAutoCollected(true);
                viral.setCollectSource("douyin_video_threshold");
                viral.setAnalysisResult(String.format("来源: douyin_video 阈值采集, videoId=%s, 播放=%d, 点赞率=%.1f%%",
                        video.getVideoId(), views, likeRate * 100));
                svViralVideoRepository.save(viral);
                created++;
            }

            log.info("[ViralCollector] douyin_video 采集完成，扫描 {} 条视频，新增 {} 条爆款",
                    videos.size(), created);
        } catch (Exception e) {
            log.error("[ViralCollector] douyin_video 采集失败: {}", e.getMessage(), e);
        }
    }

    /**
     * LF-05：垂类爆款采集（行业关键词过滤热搜 + douyin_video 标签匹配）
     */
    @Scheduled(cron = "${app.viral-collector.vertical-cron:0 0 5 * * ?}")
    public void collectVerticalVirals() {
        if (isDisabled("垂类爆款采集")) {
            return;
        }
        log.info("[垂类采集] 开始");
        try {
            if (verticalCollectorProperties == null) {
                return;
            }
            List<String> keywords = verticalCollectorProperties.getIndustryKeywords();
            if (keywords == null || keywords.isEmpty()) {
                return;
            }
            long minHeat = verticalCollectorProperties.getMinHeatScore();
            int batchLimit = verticalCollectorProperties.getBatchLimit();
            int collected = 0;

            List<SvHotTopic> topics = svHotTopicRepository.findAll();
            for (SvHotTopic topic : topics) {
                if (collected >= batchLimit) {
                    break;
                }
                if (!"active".equals(topic.getStatus())) {
                    continue;
                }
                if (topic.getHeatScore() == null || topic.getHeatScore() < minHeat) {
                    continue;
                }
                String title = topic.getTitle();
                if (!matchesVerticalKeyword(title, keywords)) {
                    continue;
                }
                String douyinKey = "topic_" + topic.getId();
                if (svViralVideoRepository.findByOwnerIdAndDouyinVideoIdAndDeleted(0L, douyinKey, 0).isPresent()) {
                    continue;
                }
                SvViralVideo viral = new SvViralVideo();
                viral.setOwnerId(0L);
                viral.setTitle(topic.getTitle());
                viral.setDouyinVideoId(douyinKey);
                viral.setTags(topic.getRelatedTags());
                viral.setViralScore(topic.getHeatScore() != null ? topic.getHeatScore().intValue() : 0);
                viral.setAutoCollected(true);
                viral.setCollectSource("vertical_industry");
                viral.setIndustryTags(buildIndustryTagsJson(title, keywords));
                viral.setRemakeStatus(0);
                viral.setAnalysisResult("来源: 垂类热搜采集, topicId=" + topic.getId());
                svViralVideoRepository.save(viral);
                collected++;
            }

            if (douyinVideoRepository != null && collected < batchLimit) {
                ShortVideoBusinessConfig.ViralThreshold threshold = shortVideoBusinessConfig.getViral();
                Specification<DouyinVideo> spec = (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("viewCount"), threshold.getMinViewCount()));
                    predicates.add(cb.equal(root.get("deleted"), 0));
                    return cb.and(predicates.toArray(new Predicate[0]));
                };
                List<DouyinVideo> videos = douyinVideoRepository.findAll(spec);
                for (DouyinVideo video : videos) {
                    if (collected >= batchLimit) {
                        break;
                    }
                    String combined = (video.getTitle() != null ? video.getTitle() : "")
                            + (video.getDescription() != null ? video.getDescription() : "");
                    if (!matchesVerticalKeyword(combined, keywords)) {
                        continue;
                    }
                    String dyKey = "dyv_" + video.getVideoId();
                    if (svViralVideoRepository.findByOwnerIdAndDouyinVideoIdAndDeleted(0L, dyKey, 0).isPresent()) {
                        continue;
                    }
                    boolean existsByTitle = svViralVideoRepository.findAll((Specification<SvViralVideo>) (root, q, cb) ->
                            cb.and(
                                    cb.equal(root.get("title"), video.getTitle()),
                                    cb.equal(root.get("deleted"), 0)
                            )).stream().findFirst().isPresent();
                    if (existsByTitle) {
                        continue;
                    }
                    long views = video.getViewCount() != null ? video.getViewCount() : 0;
                    long likes = video.getLikeCount() != null ? video.getLikeCount() : 0;
                    double likeRate = views > 0 ? (double) likes / views : 0;
                    if (likeRate < threshold.getMinLikeRate()) {
                        continue;
                    }
                    SvViralVideo viral = new SvViralVideo();
                    viral.setOwnerId(0L);
                    viral.setTitle(video.getTitle());
                    viral.setDouyinVideoId(dyKey);
                    viral.setViewCount(views);
                    viral.setLikeCount(likes);
                    viral.setShareCount(video.getShareCount() != null ? video.getShareCount() : 0);
                    viral.setViralScore((int) Math.min(100, views / 100000));
                    viral.setAutoCollected(true);
                    viral.setCollectSource("vertical_industry");
                    viral.setIndustryTags(buildIndustryTagsJson(video.getTitle(), keywords));
                    viral.setRemakeStatus(0);
                    viral.setAnalysisResult(String.format("来源: 垂类 douyin_video, videoId=%s, 播放=%d",
                            video.getVideoId(), views));
                    svViralVideoRepository.save(viral);
                    collected++;
                }
            }

            log.info("[垂类采集] 完成，新增 {} 条垂类爆款", collected);
        } catch (Exception e) {
            log.error("[垂类采集] 失败: {}", e.getMessage(), e);
        }
    }

    private static boolean matchesVerticalKeyword(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String kw : keywords) {
            if (kw != null && !kw.isEmpty() && text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static String buildIndustryTagsJson(String title, List<String> keywords) {
        List<String> matched = keywords.stream()
                .filter(kw -> kw != null && !kw.isEmpty() && title != null && title.contains(kw))
                .toList();
        try {
            return OBJECT_MAPPER.writeValueAsString(matched);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 判断视频是否符合爆款阈值
     */
    public boolean isViral(long viewCount, double likeRate, double completionRate, double shareRate) {
        ShortVideoBusinessConfig.ViralThreshold t = shortVideoBusinessConfig.getViral();
        return viewCount >= t.getMinViewCount()
                && likeRate >= t.getMinLikeRate()
                && completionRate >= t.getMinCompletionRate()
                && shareRate >= t.getMinShareRate();
    }

    private boolean isDisabled(String taskName) {
        if (collectorEnabled) {
            return false;
        }
        log.debug("[ViralCollector] 定时任务已禁用，跳过 {}", taskName);
        return true;
    }
}
