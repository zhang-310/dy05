package cn.gaifan.douyinOperations.module.ai.service.official.impl;

import cn.gaifan.douyinOperations.module.ai.config.official.DouyinSchoolCollectorProperties;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.OfficialKnowledgeCollectItemService;
import cn.gaifan.douyinOperations.module.ai.service.official.DouyinSchoolCollectorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DouyinSchoolCollectorServiceImpl implements DouyinSchoolCollectorService {

    private static final Logger log = LoggerFactory.getLogger(DouyinSchoolCollectorServiceImpl.class);
    private static final String SOURCE_TYPE = "douyin_school_official";
    private static final String CONTENT_TYPE_RULE = "official_rule";
    private static final String CONTENT_TYPE_LEARNING = "official_learning";
    private static final AtomicReference<CollectorProgress> PROGRESS = new AtomicReference<>(CollectorProgress.idle());
    private static final Map<String, TopicProfile> TOPIC_PROFILES = topicProfiles();
    private static final Pattern LINK_PATTERN = Pattern.compile("(?i)(?:href|src)\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])(/doudian/web/(?:article|video-article|course-series|rules|funcs|ecomcase|help|topic)[^\"'`\\\\\\s<)]*)");
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern ELEMENT_WITH_MEDIA_TEXT_PATTERN = Pattern.compile(
            "(?is)<(?:img|video|source|track|a|meta)\\b([^>]*)>");
    private static final Pattern ATTR_PATTERN = Pattern.compile(
            "(?is)([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?:)?//[^\"'\\s<>]+|/(?:tos|obj|api|doudian|eschool|static|video|image)/[^\"'\\s<>]+");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE_PATTERN = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SUBTITLE_TIMESTAMP_PATTERN = Pattern.compile(
            "^\\s*(?:\\d{1,2}:)?\\d{1,2}:\\d{2}[,.]\\d{1,3}\\s*-->\\s*(?:\\d{1,2}:)?\\d{1,2}:\\d{2}[,.]\\d{1,3}.*$");
    private static final Pattern SUBTITLE_SEQUENCE_PATTERN = Pattern.compile("^\\s*\\d+\\s*$");
    private static final List<String> DETAIL_TEXT_FIELDS = List.of(
            "name", "title", "content", "format_description", "plain_description", "description",
            "summary", "abstract", "introduction", "subtitle", "caption", "transcript", "video_text",
            "videoText", "subtitle_text", "subtitleText", "caption_text", "captionText", "transcript_text",
            "transcriptText", "asr_text", "asrText", "speech_text", "speechText", "voice_text",
            "voiceText", "dialogue", "script", "segments", "lines", "text", "fileName", "name",
            "lecturer", "creator_name", "modifier_name"
    );
    private static final List<String> IMAGE_URL_FIELDS = List.of(
            "fileSrc", "coverUrl", "cover_url", "cover_image_url", "poster_url", "url", "src", "uri",
            "href", "image", "image_url", "img_url", "thumb_url", "thumbnail"
    );
    private static final List<String> VIDEO_URL_FIELDS = List.of(
            "fileSrc", "main_url", "backup_url", "video_url", "play_url", "live_url", "url", "src",
            "href", "activity_url", "activity_pc_url"
    );
    private static final List<String> SUBTITLE_URL_FIELDS = List.of(
            "subtitle_url", "subtitleUrl", "subtitle_file", "subtitleFile", "subtitle_file_src",
            "subtitleFileSrc", "caption_url", "captionUrl", "captions_url", "captionsUrl",
            "transcript_url", "transcriptUrl", "track_url", "trackUrl", "track_src", "trackSrc",
            "srt_url", "srtUrl", "vtt_url", "vttUrl"
    );

    private static final List<String> TITLE_VIOLATION_KEYWORDS = List.of(
            "违规", "处罚", "封禁", "禁售", "禁发", "禁用", "扣分", "整改", "处置",
            "违规案例", "规则红线", "禁止商品", "禁止信息", "平台禁止", "发布违禁",
            "虚假宣传", "夸大宣传", "绝对化", "低俗", "色情", "暴力", "血腥", "赌博", "毒品",
            "侵权", "盗版", "假冒", "三无", "商品违规", "违规营销", "站外引流", "诱导互动",
            "诱导关注", "诱导好评", "诱导第三方", "刷单", "刷量", "搬运", "抄袭", "素材违规",
            "未授权", "直播违规", "短视频违规", "千川素材违规", "高频违规", "描述不当",
            "价格诱导", "价格虚假", "不良价值观", "引人不适", "演戏炒作", "功效虚假",
            "宣传不符", "官网版权侵权", "知识产权", "创作者信用分", "商家体验分扣分",
            "不当使用他人权利", "不平等交易", "直播间违规", "直播规范", "直播规则",
            "短视频规范", "短视频规则", "短视频挂车违规", "短视频带货违规", "千川审核",
            "素材审核不通过", "广告素材违规", "商品宣传违规", "功效宣传违规", "价格宣传违规",
            "营销宣传违规", "内容安全", "平台治理", "治理公告", "规则解读", "处罚申诉",
            "信用分", "体验分"
    );

    private static final List<String> BODY_VIOLATION_KEYWORDS = List.of(
            "直播违规", "短视频违规", "素材违规", "千川素材违规", "违规营销", "商品违规", "商品违规发布",
            "高频违规", "违规案例", "违规原因", "违规行为", "违规风险", "违规处置", "处罚措施",
            "平台处罚", "封禁账号", "禁售商品", "禁发商品", "禁止发布", "禁止信息", "发布违禁",
            "站外引流", "诱导关注", "诱导互动", "诱导好评", "诱导第三方", "刷单", "刷量",
            "虚假宣传", "虚假营销宣传", "价格虚假", "价格诱导", "不良价值观", "引人不适",
            "演戏炒作", "功效虚假", "宣传不符", "侵权", "未授权", "知识产权", "假冒", "盗版",
            "低俗", "色情", "赌博", "毒品", "不当使用他人权利", "不平等交易",
            "直播间违规", "直播规范", "直播规则", "短视频规范", "短视频规则",
            "短视频挂车违规", "短视频带货违规", "千川审核", "广告素材违规", "素材审核",
            "商品宣传违规", "功效宣传违规", "价格宣传违规", "营销宣传违规", "内容安全",
            "平台治理", "治理动态", "规则解读", "处罚申诉", "信用分", "体验分"
    );

    private static final List<String> VIOLATION_CONTEXT_KEYWORDS = List.of(
            "违规", "处罚", "封禁", "禁售", "禁发", "禁止", "扣分", "整改", "处置", "风险", "红线",
            "虚假宣传", "侵权", "站外引流", "诱导", "违禁", "低俗", "色情", "赌博", "毒品",
            "素材违规", "短视频违规", "直播违规", "直播规范", "短视频规范", "千川审核",
            "广告素材", "内容安全", "平台治理", "规则解读", "处罚申诉"
    );

    private static final List<String> SHORT_VIDEO_KEYWORDS = List.of(
            "短视频", "视频素材", "素材", "千川", "巨量千川", "封面", "标题", "搬运", "混剪", "剪辑",
            "脚本", "文案", "口播", "挂车", "带货视频", "短视频带货", "短视频运营", "短视频规则",
            "短视频规范", "素材审核", "广告素材", "图文素材", "视频号", "完播", "互动率"
    );

    private static final List<String> LIVE_KEYWORDS = List.of(
            "直播", "直播间", "主播", "助播", "场控", "中控", "连麦", "福袋", "憋单",
            "排品", "选品", "过品", "讲解", "话术", "转化", "停留", "互动", "直播运营",
            "直播规则", "直播规范", "直播违规", "控场"
    );

    private static final Map<Integer, String> OBJ_TYPE_LABELS = Map.ofEntries(
            Map.entry(1, "文章"),
            Map.entry(2, "视频"),
            Map.entry(3, "课程"),
            Map.entry(4, "直播"),
            Map.entry(5, "直播系列"),
            Map.entry(7, "知识"),
            Map.entry(10, "帖子"),
            Map.entry(11, "问题"),
            Map.entry(12, "回答"),
            Map.entry(14, "课程"),
            Map.entry(15, "短视频"),
            Map.entry(16, "文档"),
            Map.entry(99, "课程")
    );

    @Resource
    private DouyinSchoolCollectorProperties properties;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OfficialKnowledgeCollectItemService collectItemService;

    @Override
    public Map<String, Object> collect() {
        return collectInternal(null);
    }

    @Override
    public Map<String, Object> collectTopic(String topicCode) {
        TopicProfile topic = resolveTopic(topicCode);
        return collectInternal(topic);
    }

    private Map<String, Object> collectInternal(TopicProfile topic) {
        Map<String, Object> result = new LinkedHashMap<>();
        long started = System.currentTimeMillis();
        Instant startedAt = Instant.now();
        List<String> seedKeywords = topic == null ? properties.getSeedKeywords() : topic.keywords();
        String runLabel = topic == null ? "full" : topic.code();
        Long userId = properties.getUserId();
        result.put("source", SOURCE_TYPE);
        result.put("topic", runLabel);
        if (topic != null) {
            result.put("topicName", topic.name());
            result.put("topicDescription", topic.description());
        }
        result.put("userId", userId);
        result.put("violationKbName", properties.getViolationKbName());
        result.put("generalKbName", properties.getGeneralKbName());
        PROGRESS.set(CollectorProgress.running("starting", startedAt, 0, 0, 0, 0, 0, 0, 0,
                "", "", "准备采集抖音学习中心官方资料：" + runLabel));

        if (!properties.isEnabled()) {
            result.put("skipped", true);
            result.put("skipReason", "app.douyin-school.collector.enabled=false");
            PROGRESS.set(CollectorProgress.finished("skipped", startedAt, 0, 0, 0, 0, 0, 0, 0,
                    "app.douyin-school.collector.enabled=false"));
            return result;
        }
        if (userId == null || userId <= 0) {
            result.put("skipped", true);
            result.put("skipReason", "app.douyin-school.collector.user-id 未配置");
            PROGRESS.set(CollectorProgress.finished("skipped", startedAt, 0, 0, 0, 0, 0, 0, 0,
                    "app.douyin-school.collector.user-id 未配置"));
            return result;
        }

        Long generalKbId = ensureKb(userId, properties.getGeneralKbName(),
                "抖音官方学习知识库：运营、课程、行业玩法、店铺/达人/短视频/直播学习资料");
        Long violationKbId = ensureKb(userId, properties.getViolationKbName(),
                "抖音官方违规规则知识库：直播违规、短视频违规、千川素材违规、商品违规、处罚与规则红线");
        result.put("generalKbId", generalKbId);
        result.put("violationKbId", violationKbId);
        if (operationalStrategyKnowledgeService != null) {
            operationalStrategyKnowledgeService.ensureSeeded(userId);
        }

        Queue<OfficialItem> queue = new ArrayDeque<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        Set<String> visitedUrls = new HashSet<>();
        List<String> errors = new ArrayList<>();

        PROGRESS.set(CollectorProgress.running("discovering", startedAt, 0, 0, 0, 0, 0, 0, 0,
                "", "", "搜索关键词和种子页，发现官方学习资料：" + runLabel));
        discoverFromOfficialSearch(queue, seenKeys, errors, startedAt, seedKeywords, runLabel);
        discoverFromSeedPages(queue, seenKeys, visitedUrls, errors, startedAt);

        int discovered = queue.size();
        markDiscovered(queue, runLabel);
        PROGRESS.set(CollectorProgress.running("processing", startedAt, discovered, 0, 0, 0, 0, 0, 0,
                "", "", "已完成发现，开始抓取详情并写入知识库"));
        log.info("抖音学习中心官方资料发现完成: discovered={}, keywords={}, seedUrls={}, maxItems={}, pageSize={}, maxPagesPerKeyword={}",
                discovered, seedKeywords.size(), properties.getSeedUrls().size(),
                properties.getMaxItems(), properties.getPageSize(), properties.getMaxPagesPerKeyword());
        int importedViolation = 0;
        int importedGeneral = 0;
        int existing = 0;
        int skipped = 0;
        int failed = 0;
        int processed = 0;
        List<Map<String, Object>> samples = new ArrayList<>();

        while (!queue.isEmpty() && importedViolation + importedGeneral + existing + skipped + failed < properties.getMaxItems()) {
            OfficialItem item = queue.poll();
            if (item == null) continue;
            processed++;
            PROGRESS.set(CollectorProgress.running("processing", startedAt, discovered, processed,
                    importedGeneral, importedViolation, existing, skipped, failed,
                    truncate(item.title(), 120), item.url(), "正在处理官方资料"));
            try {
                OfficialItem enriched = enrichItem(item, visitedUrls);
                boolean candidateViolation = isViolation(enriched);
                enriched = completeExpensiveMediaExtraction(enriched, candidateViolation);
                if (!isImportable(enriched)) {
                    skipped++;
                    markSkipped(enriched, runLabel, "内容过短、非内容页或不匹配采集关键词");
                    continue;
                }
                boolean violation = candidateViolation || isViolation(enriched);
                if (violation && !candidateViolation) {
                    enriched = completeExpensiveMediaExtraction(enriched, true);
                }
                Long targetKbId = violation ? violationKbId : generalKbId;
                String contentType = violation ? CONTENT_TYPE_RULE : CONTENT_TYPE_LEARNING;
                List<Long> oldDocIds = oldOfficialDocumentIds(targetKbId, enriched.url());
                long uploadStartedAt = System.currentTimeMillis();
                AiKbDocument doc = knowledgeBaseService.uploadDocument(
                        targetKbId,
                        truncate(enriched.title(), 240),
                        toMarkdown(enriched, violation),
                        "md",
                        userId,
                        SOURCE_TYPE,
                        contentType,
                        metadata(enriched, violation)
                );
                if (doc != null) {
                    boolean newlyCreated = isNewlyCreated(doc, uploadStartedAt);
                    markIndexed(enriched, runLabel, violation, targetKbId, doc.getId());
                    if (newlyCreated) {
                        deleteOldOfficialDocuments(oldDocIds, userId, enriched.url(), doc.getId());
                        if (violation) {
                            importedViolation++;
                        } else {
                            importedGeneral++;
                        }
                    } else {
                        existing++;
                    }
                    if (newlyCreated && samples.size() < 20) {
                        samples.add(Map.of(
                                "docId", doc.getId(),
                                "kb", violation ? properties.getViolationKbName() : properties.getGeneralKbName(),
                                "title", enriched.title(),
                                "sourceUrl", enriched.url(),
                                "tags", classifyTags(enriched, violation)
                        ));
                    }
                }
            } catch (Exception e) {
                if (isNoIndexableOrDuplicate(e)) {
                    skipped++;
                    markSkipped(item, runLabel, e.getMessage());
                    log.debug("抖音学习中心资料跳过: title={}, reason={}", item.title(), e.getMessage());
                } else {
                    failed++;
                    markFailed(item, runLabel, e.getMessage());
                    if (errors.size() < 20) {
                        errors.add(item.title() + ": " + e.getMessage());
                    }
                    log.warn("抖音学习中心资料入库失败: title={}, url={}, err={}", item.title(), item.url(), e.getMessage());
                }
            }
            if (shouldLogProgress(processed)) {
                log.info("抖音学习中心官方资料采集中: processed={}/{}, importedGeneral={}, importedViolation={}, existing={}, skipped={}, failed={}",
                        processed, discovered, importedGeneral, importedViolation, existing, skipped, failed);
            }
            PROGRESS.set(CollectorProgress.running("processing", startedAt, discovered, processed,
                    importedGeneral, importedViolation, existing, skipped, failed,
                    truncate(item.title(), 120), item.url(), "正在处理官方资料"));
            sleepQuietly(properties.getRequestDelayMs());
        }

        result.put("discovered", discovered);
        result.put("importedViolation", importedViolation);
        result.put("importedGeneral", importedGeneral);
        result.put("existing", existing);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("durationMs", System.currentTimeMillis() - started);
        result.put("samples", samples);
        result.put("errors", errors);
        PROGRESS.set(CollectorProgress.finished("completed", startedAt, discovered, processed,
                importedGeneral, importedViolation, existing, skipped, failed, "采集完成"));
        log.info("抖音学习中心官方资料采集完成: discovered={}, violation={}, general={}, existing={}, skipped={}, failed={}, durationMs={}",
                discovered, importedViolation, importedGeneral, existing, skipped, failed, System.currentTimeMillis() - started);
        return result;
    }

    @Override
    public Map<String, Object> topics() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> topics = TOPIC_PROFILES.values().stream()
                .map(topic -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", topic.code());
                    m.put("name", topic.name());
                    m.put("description", topic.description());
                    m.put("keywordCount", topic.keywords().size());
                    m.put("keywords", topic.keywords());
                    return m;
                })
                .toList();
        result.put("topics", topics);
        result.put("count", topics.size());
        return result;
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>(PROGRESS.get().toMap());
        status.put("sourceType", SOURCE_TYPE);
        status.put("generalKbName", properties.getGeneralKbName());
        status.put("violationKbName", properties.getViolationKbName());
        status.put("enabled", properties.isEnabled());
        status.put("cron", properties.getCron());
        status.put("maxItems", properties.getMaxItems());
        status.put("pageSize", properties.getPageSize());
        status.put("maxPagesPerKeyword", properties.getMaxPagesPerKeyword());
        status.put("deepMediaExtractionEnabled", properties.isDeepMediaExtractionEnabled());
        status.put("imageOcrEnabled", properties.isImageOcrEnabled());
        status.put("videoAsrEnabled", properties.isVideoAsrEnabled());
        status.put("officialDocumentCount", documentRepository.countBySourceTypeAndDeleted(SOURCE_TYPE, 0));
        if (collectItemService != null) {
            status.put("collectLedger", collectItemService.summary());
        }
        return status;
    }

    private Long ensureKb(Long userId, String kbName, String description) {
        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, kbName);
        if (kbId != null) return kbId;
        return knowledgeBaseService.createKnowledgeBase(kbName, description, userId).getId();
    }

    private TopicProfile resolveTopic(String topicCode) {
        String code = normalizeTopicCode(topicCode);
        TopicProfile topic = TOPIC_PROFILES.get(code);
        if (topic == null) {
            throw new IllegalArgumentException("未知抖音学习中心专题: " + topicCode + "，可用专题: " + TOPIC_PROFILES.keySet());
        }
        return topic;
    }

    private static Map<String, TopicProfile> topicProfiles() {
        Map<String, TopicProfile> topics = new LinkedHashMap<>();
        addTopic(topics, "short_video", "短视频体系补强",
                "短视频运营、创作、脚本、文案、挂车、封面、剪辑、带货规则与违规点",
                List.of(
                        "短视频", "短视频运营", "短视频创作", "短视频脚本", "短视频文案",
                        "短视频标题", "短视频封面", "短视频剪辑", "短视频带货", "短视频挂车",
                        "短视频规则", "短视频规范", "短视频违规", "短视频违规案例", "短视频素材违规",
                        "带货视频", "商品图文", "图文素材", "视频素材", "口播", "完播率",
                        "互动率", "内容营销", "流量获取", "搜索运营", "商城运营"
                ));
        addTopic(topics, "live_violation", "直播违规补强",
                "直播间违规、直播规范、话术红线、站外引流、诱导互动、虚假宣传和处罚规则",
                List.of(
                        "直播违规", "直播间违规", "直播规范", "直播规则", "直播带货规则",
                        "直播话术违规", "直播营销违规", "直播虚假宣传", "直播站外引流",
                        "直播诱导互动", "直播低俗", "直播高频违规", "主播违规", "助播违规",
                        "场控违规", "福袋违规", "连麦违规", "违规营销", "处罚", "整改",
                        "封禁", "扣分", "创作者信用分", "体验分", "规则解读"
                ));
        addTopic(topics, "qianchuan_material", "千川素材补强",
                "巨量千川、广告素材审核、素材规范、素材违规、图文/视频素材和投放内容安全",
                List.of(
                        "千川", "巨量千川", "千川素材", "千川素材审核", "千川素材违规",
                        "广告素材", "广告素材审核", "广告素材违规", "素材审核", "素材规范",
                        "素材违规", "视频素材", "图文素材", "商品素材", "投放素材",
                        "创意素材", "素材审核不通过", "内容安全", "营销宣传违规", "虚假宣传",
                        "功效虚假", "价格虚假", "知识产权", "侵权"
                ));
        addTopic(topics, "product_compliance", "商品宣传违规补强",
                "商品发布、标题、主图、详情、价格、功效、行业规范、禁售和宣传合规",
                List.of(
                        "商品违规发布", "商品发布规则", "商品标题", "商品主图", "商品详情",
                        "商品宣传", "商品宣传违规", "价格宣传", "价格虚假", "价格诱导",
                        "功效宣传", "功效虚假", "虚假宣传", "夸大宣传", "绝对化用语",
                        "禁售", "禁发", "禁止发布", "行业管理规范", "食品健康",
                        "美妆", "生鲜", "3C数码家电", "不当使用他人权利", "知识产权"
                ));
        return Map.copyOf(topics);
    }

    private static void addTopic(Map<String, TopicProfile> topics, String code, String name,
                                 String description, List<String> keywords) {
        topics.put(code, new TopicProfile(code, name, description,
                keywords.stream().filter(StringUtils::hasText).distinct().toList()));
    }

    private static String normalizeTopicCode(String topicCode) {
        return topicCode == null ? "" : topicCode.trim().toLowerCase(Locale.ROOT).replace("-", "_");
    }

    private List<Long> oldOfficialDocumentIds(Long kbId, String sourceUrl) {
        if (kbId == null || !StringUtils.hasText(sourceUrl)) return List.of();
        String pattern = "%" + sourceUrl.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return documentRepository.findByKbSourceTypeAndMetadataSourceUrl(
                        kbId, SOURCE_TYPE, pattern, PageRequest.of(0, 5))
                .stream()
                .map(AiKbDocument::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private void deleteOldOfficialDocuments(List<Long> oldDocIds, Long userId, String sourceUrl, Long newDocId) {
        if (oldDocIds == null || oldDocIds.isEmpty()) return;
        for (Long oldDocId : oldDocIds) {
            if (oldDocId == null || oldDocId.equals(newDocId)) continue;
            try {
                knowledgeBaseService.deleteDocument(oldDocId, userId);
            } catch (Exception e) {
                log.warn("抖音学习中心旧官方文档替换删除失败 docId={}, url={}, err={}",
                        oldDocId, sourceUrl, e.getMessage());
            }
        }
    }

    private OfficialItem completeExpensiveMediaExtraction(OfficialItem item, boolean violation) {
        if (item == null) return null;
        if (!properties.isDeepMediaExtractionEnabled()) return item;
        MediaCollector collector = new MediaCollector(item.url());
        item.imageUrls().forEach(collector::addImageUrl);
        item.videoUrls().forEach(collector::addVideoUrl);
        item.imageTexts().forEach(collector::addImageText);
        item.videoTexts().forEach(collector::addVideoText);
        item.mediaExtractionStatuses().forEach(collector::addMediaStatus);
        if (properties.isImageOcrEnabled()) {
            collectImageOcr(collector, violation);
        }
        if (properties.isVideoAsrEnabled()) {
            collectVideoAsr(collector);
        }
        return item.merge(null, null, collector.toInsights());
    }

    private void discoverFromOfficialSearch(Queue<OfficialItem> queue, Set<String> seenKeys, List<String> errors,
                                            Instant startedAt, List<String> seedKeywords, String runLabel) {
        int keywordIndex = 0;
        List<String> keywords = seedKeywords == null ? List.of() : seedKeywords;
        for (String keyword : keywords) {
            keywordIndex++;
            int beforeKeyword = seenKeys.size();
            for (int page = 0; page < properties.getMaxPagesPerKeyword(); page++) {
                if (seenKeys.size() >= properties.getMaxItems()) return;
                PROGRESS.set(CollectorProgress.running("discovering", startedAt,
                        seenKeys.size(), keywordIndex, 0, 0, 0, 0, 0,
                        keyword + " 第 " + (page + 1) + " 页", "",
                        "正在搜索专题 " + runLabel + " 关键词 " + keywordIndex + "/" + keywords.size()
                                + "，已发现 " + seenKeys.size() + " 条官方资料"));
                try {
                    URI uri = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl() + properties.getSearchPath())
                            .queryParam("keyword", keyword)
                            .queryParam("query", keyword)
                            .queryParam("search_word", keyword)
                            .queryParam("obj_types", "1,2,3,4,5,7,14,15,16,99")
                            .queryParam("offset", page * properties.getPageSize())
                            .queryParam("count", properties.getPageSize())
                            .queryParam("page", page + 1)
                            .queryParam("page_size", properties.getPageSize())
                            .build()
                            .encode(StandardCharsets.UTF_8)
                            .toUri();
                    String json = fetch(uri.toString(), MediaType.APPLICATION_JSON);
                    int added = parseSearchItems(json, keyword, queue, seenKeys);
                    if (page == 0 || added > 0 || ((page + 1) % 5 == 0)) {
                        log.info("抖音学习中心搜索发现: topic={}, keyword={}/{} {}, page={}, added={}, totalSeen={}",
                                runLabel, keywordIndex, keywords.size(), keyword, page + 1, added, seenKeys.size());
                    }
                    PROGRESS.set(CollectorProgress.running("discovering", startedAt,
                            seenKeys.size(), keywordIndex, 0, 0, 0, 0, 0,
                            keyword + " 第 " + (page + 1) + " 页", uri.toString(),
                            "正在搜索专题 " + runLabel + " 关键词 " + keywordIndex + "/" + keywords.size()
                                    + "，已发现 " + seenKeys.size() + " 条官方资料"));
                    if (added == 0 && page > 0) break;
                    sleepQuietly(properties.getRequestDelayMs());
                } catch (Exception e) {
                    if (errors.size() < 20) {
                        errors.add("search[" + keyword + "]: " + e.getMessage());
                    }
                    log.warn("抖音学习中心搜索采集失败 keyword={}, page={}, addedByKeyword={}, err={}",
                            keyword, page + 1, seenKeys.size() - beforeKeyword, e.getMessage());
                    break;
                }
            }
            if (seenKeys.size() - beforeKeyword > 0) {
                log.info("抖音学习中心关键词发现完成: keyword={}, added={}, totalSeen={}",
                        keyword, seenKeys.size() - beforeKeyword, seenKeys.size());
            }
        }
    }

    private int parseSearchItems(String json, String keyword, Queue<OfficialItem> queue, Set<String> seenKeys) throws Exception {
        if (!StringUtils.hasText(json) || !json.trim().startsWith("{")) return 0;
        JsonNode root = objectMapper.readTree(json);
        if (root.path("code").asInt(-1) != 0) return 0;
        JsonNode data = root.path("data");
        int before = seenKeys.size();
        collectArrayItems(data.path("articles"), keyword, queue, seenKeys);
        collectArrayItems(data.path("courses"), keyword, queue, seenKeys);
        collectArrayItems(data.path("videos"), keyword, queue, seenKeys);
        collectArrayItems(data.path("lives"), keyword, queue, seenKeys);
        collectNestedArrays(data, keyword, queue, seenKeys, 0);
        return seenKeys.size() - before;
    }

    private void collectNestedArrays(JsonNode node, String keyword, Queue<OfficialItem> queue, Set<String> seenKeys, int depth) {
        if (node == null || node.isMissingNode() || depth > 3 || seenKeys.size() >= properties.getMaxItems()) return;
        if (node.isArray()) {
            collectArrayItems(node, keyword, queue, seenKeys);
            for (JsonNode child : node) {
                collectNestedArrays(child, keyword, queue, seenKeys, depth + 1);
            }
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> collectNestedArrays(e.getValue(), keyword, queue, seenKeys, depth + 1));
        }
    }

    private void collectArrayItems(JsonNode array, String keyword, Queue<OfficialItem> queue, Set<String> seenKeys) {
        if (array == null || !array.isArray()) return;
        for (JsonNode node : array) {
            if (seenKeys.size() >= properties.getMaxItems()) return;
            OfficialItem item = toOfficialItem(node, keyword);
            if (item == null) continue;
            addItem(queue, seenKeys, item);
        }
    }

    private OfficialItem toOfficialItem(JsonNode node, String keyword) {
        if (node == null || !node.isObject()) return null;
        String title = firstText(node, "name", "title", "article_title", "course_name", "lesson_name");
        String content = mergeContent(
                firstText(node, "content", "summary", "abstract", "description", "desc", "subtitle", "introduction"),
                extractTextFromJson(node, 0)
        );
        String id = firstText(node, "id", "article_id", "lesson_id", "plan_id", "object_id", "obj_id");
        int objType = node.path("obj_type").asInt(0);
        String url = firstText(node, "url", "link", "target_url", "jump_url", "schema", "href");
        if (!StringUtils.hasText(url)) {
            url = buildOfficialUrl(id, objType);
        }
        if (!StringUtils.hasText(title) && !StringUtils.hasText(content)) return null;
        if (!StringUtils.hasText(url) && !StringUtils.hasText(id)) return null;
        String category = OBJ_TYPE_LABELS.getOrDefault(objType, StringUtils.hasText(keyword) ? keyword : "官方资料");
        List<String> tags = new ArrayList<>();
        tags.add("official");
        JsonNode tagsNode = node.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode t : tagsNode) {
                if (t.isTextual() && StringUtils.hasText(t.asText())) tags.add(t.asText());
                else if (t.isObject()) {
                    String tv = firstText(t, "name", "title", "tag_name");
                    if (StringUtils.hasText(tv)) tags.add(tv);
                }
            }
        }
        long updatedAt = node.path("update_timestamp").asLong(0);
        long createdAt = node.path("create_timestamp").asLong(0);
        MediaInsights mediaInsights = extractMediaFromJson(node, url, false);
        return new OfficialItem(clean(title), clean(content), normalizeUrl(url), id, objType, category,
                tags.stream().filter(StringUtils::hasText).distinct().toList(), updatedAt, createdAt,
                mediaInsights.imageTexts(), mediaInsights.videoTexts(), mediaInsights.imageUrls(), mediaInsights.videoUrls(),
                mediaInsights.mediaExtractionStatuses());
    }

    private String buildOfficialUrl(String id, int objType) {
        if (!StringUtils.hasText(id)) return null;
        String path = switch (objType) {
            case 2, 15 -> "/doudian/web/video-article/" + id;
            case 3, 14, 99 -> "/doudian/web/course-series/" + id;
            default -> "/doudian/web/article/" + id;
        };
        return normalizeBaseUrl() + path;
    }

    private void discoverFromSeedPages(Queue<OfficialItem> queue, Set<String> seenKeys, Set<String> visitedUrls,
                                       List<String> errors, Instant startedAt) {
        for (String seed : properties.getSeedUrls()) {
            if (!StringUtils.hasText(seed) || seenKeys.size() >= properties.getMaxItems()) return;
            int before = seenKeys.size();
            try {
                PROGRESS.set(CollectorProgress.running("discovering_seed_pages", startedAt,
                        seenKeys.size(), visitedUrls.size(), 0, 0, 0, 0, 0,
                        "种子页发现", seed, "正在从学习中心种子页发现更多官方资料"));
                discoverLinks(seed, queue, seenKeys, visitedUrls, 0);
                log.info("抖音学习中心种子页发现: seed={}, added={}, totalSeen={}, visitedUrls={}",
                        seed, seenKeys.size() - before, seenKeys.size(), visitedUrls.size());
                PROGRESS.set(CollectorProgress.running("discovering_seed_pages", startedAt,
                        seenKeys.size(), visitedUrls.size(), 0, 0, 0, 0, 0,
                        "种子页发现", seed, "种子页发现完成，已发现 " + seenKeys.size() + " 条官方资料"));
                sleepQuietly(properties.getRequestDelayMs());
            } catch (Exception e) {
                if (errors.size() < 20) errors.add("seed[" + seed + "]: " + e.getMessage());
                log.warn("抖音学习中心种子页发现失败 seed={}, err={}", seed, e.getMessage());
            }
        }
    }

    private void discoverLinks(String url, Queue<OfficialItem> queue, Set<String> seenKeys, Set<String> visitedUrls, int depth) {
        if (depth > 1 || !StringUtils.hasText(url) || !visitedUrls.add(url)) return;
        String html = fetch(url, MediaType.TEXT_HTML);
        if (isOfficialContentUrl(url)) {
            addPageAsItem(url, html, queue, seenKeys);
        }

        for (String link : extractLinks(html, url)) {
            if (seenKeys.size() >= properties.getMaxItems()) return;
            if (isOfficialContentUrl(link)) {
                addItem(queue, seenKeys, new OfficialItem(titleFromUrl(link), "", normalizeUrl(link),
                        idFromUrl(link), objTypeFromUrl(link), categoryFromUrl(link), List.of("official", "page"), 0, 0,
                        List.of(), List.of(), List.of(), List.of(), List.of()));
            } else if (isSeedLikeUrl(link)) {
                discoverLinks(link, queue, seenKeys, visitedUrls, depth + 1);
            }
        }
    }

    private void addPageAsItem(String url, String html, Queue<OfficialItem> queue, Set<String> seenKeys) {
        String text = extractText(html);
        if (!StringUtils.hasText(text) || text.length() < properties.getMinContentLength()) return;
        String title = extractTitle(html);
        MediaInsights mediaInsights = extractMediaFromHtml(html, url, false);
        addItem(queue, seenKeys, new OfficialItem(
                StringUtils.hasText(title) ? title : titleFromUrl(url),
                text,
                normalizeUrl(url),
                idFromUrl(url),
                objTypeFromUrl(url),
                categoryFromUrl(url),
                List.of("official", "page"),
                0,
                0,
                mediaInsights.imageTexts(),
                mediaInsights.videoTexts(),
                mediaInsights.imageUrls(),
                mediaInsights.videoUrls(),
                mediaInsights.mediaExtractionStatuses()
        ));
    }

    private OfficialItem enrichItem(OfficialItem item, Set<String> visitedUrls) {
        if (item == null || !StringUtils.hasText(item.url()) || visitedUrls.contains(item.url())) return item;
        if (!item.url().contains("/doudian/web/")) return item;
        OfficialItem enriched = enrichFromOfficialApis(item);
        try {
            visitedUrls.add(enriched.url());
            String html = fetch(enriched.url(), MediaType.TEXT_HTML);
            String text = extractText(html);
            String title = extractTitle(html);
        MediaInsights htmlInsights = extractMediaFromHtml(html, enriched.url(), false);
            return enriched.merge(
                    StringUtils.hasText(enriched.title()) ? enriched.title() : title,
                    text.length() >= properties.getMinContentLength()
                            && (!StringUtils.hasText(enriched.content()) || text.length() > enriched.content().length())
                            ? text
                            : enriched.content(),
                    htmlInsights
            );
        } catch (Exception e) {
            log.debug("抖音学习中心页面正文拉取失败 url={}, err={}", enriched.url(), e.getMessage());
        }
        return enriched;
    }

    private boolean isImportable(OfficialItem item) {
        if (item == null) return false;
        if (!StringUtils.hasText(item.title()) && !StringUtils.hasText(item.content())) return false;
        if (!StringUtils.hasText(item.content()) || effectiveContentLength(item.content()) < properties.getMinContentLength()) {
            return false;
        }
        return containsAny(item.title() + "\n" + item.content(), properties.getSeedKeywords())
                || isOfficialContentUrl(item.url());
    }

    private boolean isViolation(OfficialItem item) {
        String title = normalizeForMatch(item.title());
        if (containsAny(title, TITLE_VIOLATION_KEYWORDS)) {
            return true;
        }
        String body = normalizeForMatch(item.combinedSearchText());
        if (containsAny(body, BODY_VIOLATION_KEYWORDS)) {
            return true;
        }
        String categoryAndTags = normalizeForMatch(item.category() + "\n" + String.join(" ", item.tags()));
        return containsAny(categoryAndTags, VIOLATION_CONTEXT_KEYWORDS)
                && containsAny(title + "\n" + body, VIOLATION_CONTEXT_KEYWORDS);
    }

    private List<String> classifyTags(OfficialItem item, boolean violation) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("official");
        tags.add("抖音电商学习中心");
        if (violation) tags.add("违规规则");
        String text = normalizeForMatch(item.title() + "\n" + item.combinedSearchText() + "\n" + String.join(" ", item.tags()));
        if (containsAny(text, LIVE_KEYWORDS)) tags.add("直播");
        if (containsAny(text, SHORT_VIDEO_KEYWORDS)) tags.add("短视频");
        if (text.contains("千川")) tags.add("千川素材");
        if (text.contains("商品")) tags.add("商品规则");
        if (text.contains("达人")) tags.add("达人");
        if (text.contains("商家")) tags.add("商家");
        if (text.contains("行业")) tags.add("行业");
        tags.addAll(item.tags());
        return tags.stream().filter(StringUtils::hasText).distinct().limit(12).toList();
    }

    private String toMarkdown(OfficialItem item, boolean violation) {
        List<String> tags = classifyTags(item, violation);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(clean(item.title())).append("\n\n");
        sb.append("- 来源：抖音电商学习中心官方资料\n");
        sb.append("- URL：").append(item.url()).append("\n");
        sb.append("- 分类：").append(violation ? "违规规则" : "抖音学习资料").append("\n");
        sb.append("- 标签：").append(String.join("、", tags)).append("\n");
        if (item.updateTimestamp() > 0) {
            sb.append("- 官方更新时间：").append(Instant.ofEpochSecond(item.updateTimestamp())).append("\n");
        }
        sb.append("\n## 官方内容\n\n");
        sb.append(clean(item.content())).append("\n");
        appendSection(sb, "规则图片文字", item.imageTexts());
        appendSection(sb, "视频文案/字幕", item.videoTexts());
        appendSection(sb, "媒体解析状态", item.mediaExtractionStatuses());
        if (!item.imageUrls().isEmpty() || !item.videoUrls().isEmpty()) {
            sb.append("\n## 媒体资源\n\n");
            for (String imageUrl : item.imageUrls()) {
                sb.append("- 图片：").append(imageUrl).append("\n");
            }
            for (String videoUrl : item.videoUrls()) {
                sb.append("- 视频：").append(videoUrl).append("\n");
            }
        }
        return sb.toString();
    }

    private Map<String, String> metadata(OfficialItem item, boolean violation) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourceUrl", item.url());
        metadata.put("sourceSite", "school.jinritemai.com");
        metadata.put("source", "official");
        metadata.put("official", "true");
        metadata.put("collector", SOURCE_TYPE);
        metadata.put("targetKb", violation ? properties.getViolationKbName() : properties.getGeneralKbName());
        metadata.put("category", violation ? "违规规则" : "抖音学习资料");
        metadata.put("contentType", violation ? CONTENT_TYPE_RULE : CONTENT_TYPE_LEARNING);
        metadata.put("tags", String.join(",", classifyTags(item, violation)));
        metadata.put("sourceId", item.sourceId() != null ? item.sourceId() : "");
        metadata.put("objType", String.valueOf(item.objType()));
        metadata.put("officialUpdateTimestamp", item.updateTimestamp() > 0 ? String.valueOf(item.updateTimestamp()) : "");
        metadata.put("collectedAt", Instant.now().toString());
        metadata.put("kbPurpose", violation ? "douyin_violation_rules" : "douyin_official_learning");
        metadata.put("imageCount", String.valueOf(item.imageUrls().size()));
        metadata.put("videoCount", String.valueOf(item.videoUrls().size()));
        metadata.put("imageTextCount", String.valueOf(item.imageTexts().size()));
        metadata.put("videoTextCount", String.valueOf(item.videoTexts().size()));
        metadata.put("imageOcrEnabled", String.valueOf(properties.isImageOcrEnabled()));
        metadata.put("videoAsrEnabled", String.valueOf(properties.isVideoAsrEnabled()));
        metadata.put("mediaExtractionStatusCount", String.valueOf(item.mediaExtractionStatuses().size()));
        metadata.put("mediaExtractionStatuses", String.join(" | ", item.mediaExtractionStatuses()));
        metadata.put("imageUrls", String.join(",", item.imageUrls()));
        metadata.put("videoUrls", String.join(",", item.videoUrls()));
        return metadata;
    }

    private void markDiscovered(Iterable<OfficialItem> items, String topicCode) {
        if (collectItemService == null || items == null) return;
        for (OfficialItem item : items) {
            try {
                collectItemService.markDiscovered(toCollectEvent(item, topicCode, false, null, null, null));
            } catch (Exception e) {
                log.debug("官方采集台账登记发现失败 url={}, err={}", item != null ? item.url() : null, e.getMessage());
            }
        }
    }

    private void markIndexed(OfficialItem item, String topicCode, boolean violation, Long kbId, Long docId) {
        if (collectItemService == null || item == null) return;
        try {
            collectItemService.markIndexed(toCollectEvent(item, topicCode, violation, kbId, docId, null));
        } catch (Exception e) {
            log.debug("官方采集台账登记索引完成失败 url={}, err={}", item.url(), e.getMessage());
        }
    }

    private void markSkipped(OfficialItem item, String topicCode, String reason) {
        if (collectItemService == null || item == null) return;
        try {
            collectItemService.markSkipped(toCollectEvent(item, topicCode, false, null, null, reason));
        } catch (Exception e) {
            log.debug("官方采集台账登记跳过失败 url={}, err={}", item.url(), e.getMessage());
        }
    }

    private void markFailed(OfficialItem item, String topicCode, String error) {
        if (collectItemService == null || item == null) return;
        try {
            collectItemService.markFailed(toCollectEvent(item, topicCode, false, null, null, error));
        } catch (Exception e) {
            log.debug("官方采集台账登记失败状态失败 url={}, err={}", item.url(), e.getMessage());
        }
    }

    private OfficialKnowledgeCollectItemService.CollectItemEvent toCollectEvent(
            OfficialItem item,
            String topicCode,
            boolean violation,
            Long kbId,
            Long docId,
            String error
    ) {
        String targetKbName = violation ? properties.getViolationKbName() : properties.getGeneralKbName();
        Map<String, String> meta = metadata(item, violation);
        meta.put("topicCode", topicCode == null ? "full" : topicCode);
        return new OfficialKnowledgeCollectItemService.CollectItemEvent(
                SOURCE_TYPE,
                "school.jinritemai.com",
                item.url(),
                item.sourceId(),
                item.title(),
                item.category(),
                topicCode == null ? "full" : topicCode,
                targetKbName,
                kbId,
                docId,
                violation,
                item.imageUrls().size(),
                item.videoUrls().size(),
                item.imageTexts().size(),
                item.videoTexts().size(),
                item.updateTimestamp(),
                toJson(meta),
                error
        );
    }

    private String toJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void addItem(Queue<OfficialItem> queue, Set<String> seenKeys, OfficialItem item) {
        if (item == null) return;
        String key = itemKey(item);
        if (!StringUtils.hasText(key) || !seenKeys.add(key)) return;
        queue.add(item);
    }

    private String itemKey(OfficialItem item) {
        if (StringUtils.hasText(item.url())) return normalizeUrl(item.url());
        if (StringUtils.hasText(item.sourceId())) return item.sourceId();
        return item.title();
    }

    private OfficialItem enrichFromOfficialApis(OfficialItem item) {
        OfficialItem enriched = item;
        try {
            if (enriched.objType() == 2 || enriched.objType() == 15 || enriched.url().contains("/video-article/")) {
                enriched = enrichFromVideoDetail(enriched);
            } else if (enriched.url().contains("/article/") || enriched.objType() == 1 || enriched.objType() == 16) {
                enriched = enrichFromArticleDetail(enriched);
            }
        } catch (Exception e) {
            log.debug("抖音学习中心官方详情接口解析失败 url={}, err={}", item.url(), e.getMessage());
        }
        return enriched;
    }

    private OfficialItem enrichFromArticleDetail(OfficialItem item) throws Exception {
        String id = StringUtils.hasText(item.sourceId()) ? item.sourceId() : idFromUrl(item.url());
        if (!StringUtils.hasText(id)) return item;
        String url = normalizeBaseUrl() + "/api/eschool/v2/library/article/detail?id="
                + encodeParam(id) + "&graphId=312&need_content=true";
        JsonNode root = objectMapper.readTree(fetch(url, MediaType.APPLICATION_JSON));
        if (root.path("code").asInt(-1) != 0) return item;
        JsonNode data = root.path("data");
        JsonNode article = data.path("article_info");
        if (!article.isObject()) return item;
        MediaInsights mediaInsights = extractMediaFromJson(data, item.url(), false);
        String title = firstText(article, "name", "title");
        String content = mergeContent(
                item.content(),
                firstText(article, "description", "plain_description"),
                extractTextFromJson(article.path("content"), 0),
                extractTextFromJson(article.path("format_description"), 0),
                extractTextFromJson(data.path("sources"), 0)
        );
        List<String> tags = mergeTags(item.tags(), article.path("tags"), article.path("search_tags"));
        long updated = article.path("update_timestamp").asLong(item.updateTimestamp());
        long created = article.path("create_timestamp").asLong(item.createTimestamp());
        return new OfficialItem(
                StringUtils.hasText(title) ? clean(title) : item.title(),
                content,
                item.url(),
                StringUtils.hasText(item.sourceId()) ? item.sourceId() : id,
                item.objType(),
                item.category(),
                tags,
                updated,
                created,
                item.imageTexts(),
                item.videoTexts(),
                item.imageUrls(),
                item.videoUrls(),
                item.mediaExtractionStatuses()
        ).merge(null, null, mediaInsights);
    }

    private OfficialItem enrichFromVideoDetail(OfficialItem item) throws Exception {
        String id = StringUtils.hasText(item.sourceId()) ? item.sourceId() : idFromUrl(item.url());
        if (!StringUtils.hasText(id)) return item;
        String url = normalizeBaseUrl() + "/api/eschool/v1/video/detail?id=" + encodeParam(id);
        JsonNode root = objectMapper.readTree(fetch(url, MediaType.APPLICATION_JSON));
        if (root.path("code").asInt(-1) != 0) return item;
        JsonNode data = root.path("data");
        if (!data.isObject()) return item;
        MediaInsights mediaInsights = extractMediaFromJson(data, item.url(), false);
        String title = firstText(data, "name", "title");
        String content = mergeContent(
                item.content(),
                firstText(data, "description", "plain_description"),
                extractTextFromJson(data.path("format_description"), 0),
                extractTextFromJson(data.path("sources"), 0)
        );
        List<String> tags = mergeTags(item.tags(), data.path("tags"), data.path("search_tags"));
        long updated = data.path("updated_at").asLong(data.path("update_timestamp").asLong(item.updateTimestamp()));
        long created = data.path("created_at").asLong(data.path("create_timestamp").asLong(item.createTimestamp()));
        return new OfficialItem(
                StringUtils.hasText(title) ? clean(title) : item.title(),
                content,
                item.url(),
                StringUtils.hasText(item.sourceId()) ? item.sourceId() : id,
                item.objType(),
                item.category(),
                tags,
                updated,
                created,
                item.imageTexts(),
                item.videoTexts(),
                item.imageUrls(),
                item.videoUrls(),
                item.mediaExtractionStatuses()
        ).merge(null, null, mediaInsights);
    }

    private MediaInsights extractMediaFromJson(JsonNode node, String baseUrl) {
        return extractMediaFromJson(node, baseUrl, true);
    }

    private MediaInsights extractMediaFromJson(JsonNode node, String baseUrl, boolean expensiveExtraction) {
        MediaCollector collector = new MediaCollector(baseUrl);
        collectMediaFromJson(node, collector, 0, "");
        if (expensiveExtraction && properties.isImageOcrEnabled()) {
            collectImageOcr(collector, false);
        }
        if (expensiveExtraction && properties.isVideoAsrEnabled()) {
            collectVideoAsr(collector);
        }
        return collector.toInsights();
    }

    private void collectMediaFromJson(JsonNode node, MediaCollector collector, int depth, String fieldName) {
        if (node == null || node.isMissingNode() || depth > 8) return;
        if (node.isTextual()) {
            String value = unescapeHtml(node.asText());
            if (!StringUtils.hasText(value)) return;
            String normalizedField = normalizeForMatch(fieldName);
            if (looksLikeJson(value)) {
                try {
                    collectMediaFromJson(objectMapper.readTree(value), collector, depth + 1, fieldName);
                    return;
                } catch (Exception ignored) {
                }
            }
            if (isImageField(normalizedField) || (looksLikeUrlReference(value) && looksLikeImageUrl(value))) {
                collector.addImageUrl(value);
            }
            if (isVideoField(normalizedField) || (looksLikeUrlReference(value) && looksLikeVideoUrl(value))) {
                collector.addVideoUrl(value);
            }
            if (isSubtitleUrlField(normalizedField)) {
                collector.addSubtitleUrl(value);
            }
            if (isUsefulTextField(normalizedField) || looksLikePlainContent(value)) {
                String text = extractReadableText(value);
                if (StringUtils.hasText(text)) {
                    if (isVideoTextField(normalizedField)) {
                        collector.addVideoText(text);
                    } else if (isImageTextField(normalizedField)) {
                        collector.addImageText(text);
                    } else {
                        collector.addContentText(text);
                    }
                }
            }
            extractUrlsFromText(value, collector);
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectMediaFromJson(child, collector, depth + 1, fieldName);
            }
            return;
        }
        if (node.isObject()) {
            collectTypedMediaObject(node, collector, fieldName);
            node.fields().forEachRemaining(e -> collectMediaFromJson(e.getValue(), collector, depth + 1, e.getKey()));
        }
    }

    private void collectTypedMediaObject(JsonNode node, MediaCollector collector, String fieldName) {
        MediaKind kind = detectMediaKind(node, fieldName);
        if (kind == MediaKind.IMAGE) {
            addObjectUrls(node, IMAGE_URL_FIELDS, collector::addImageUrl);
            addObjectText(node, collector::addImageText, "图片");
        } else if (kind == MediaKind.VIDEO) {
            addObjectUrls(node, VIDEO_URL_FIELDS, collector::addVideoUrl);
            addObjectUrls(node, SUBTITLE_URL_FIELDS, collector::addSubtitleUrl);
            addObjectText(node, collector::addVideoText, "视频");
        }
    }

    private void addObjectUrls(JsonNode node, List<String> fieldNames, java.util.function.Consumer<String> consumer) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                consumer.accept(value.asText());
            }
        }
    }

    private void addObjectText(JsonNode node, java.util.function.Consumer<String> consumer, String label) {
        for (String fieldName : List.of(
                "fileName", "name", "title", "alt", "caption", "subtitle", "transcript",
                "video_text", "videoText", "subtitle_text", "subtitleText", "caption_text", "captionText",
                "transcript_text", "transcriptText", "asr_text", "asrText", "speech_text", "speechText",
                "voice_text", "voiceText", "dialogue", "script", "plain_description", "description",
                "format_description")) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                String text = extractReadableText(value.asText());
                if (StringUtils.hasText(text)) {
                    consumer.accept(label + "说明：" + text);
                }
            }
        }
    }

    private MediaKind detectMediaKind(JsonNode node, String fieldName) {
        String text = normalizeForMatch(fieldName + " "
                + firstText(node, "fileType", "type", "mime_type", "media_type", "kind", "resource_type"));
        if (isTruthy(node.path("VIDEO")) || text.contains("video") || text.contains("视频")
                || node.has("video_play_info") || node.has("video_infos")) {
            return MediaKind.VIDEO;
        }
        if (isTruthy(node.path("IMAGE")) || text.contains("image") || text.contains("图片")) {
            return MediaKind.IMAGE;
        }
        return MediaKind.UNKNOWN;
    }

    private MediaInsights extractMediaFromHtml(String html, String baseUrl) {
        return extractMediaFromHtml(html, baseUrl, true);
    }

    private MediaInsights extractMediaFromHtml(String html, String baseUrl, boolean expensiveExtraction) {
        MediaCollector collector = new MediaCollector(baseUrl);
        if (!StringUtils.hasText(html)) return collector.toInsights();
        Matcher elementMatcher = ELEMENT_WITH_MEDIA_TEXT_PATTERN.matcher(html);
        while (elementMatcher.find()) {
            Map<String, String> attrs = parseAttributes(elementMatcher.group(1));
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                String key = normalizeForMatch(entry.getKey());
                String value = unescapeHtml(entry.getValue());
                if (isImageField(key) || looksLikeImageUrl(value)) {
                    collector.addImageUrl(value);
                }
                if (isVideoField(key) || looksLikeVideoUrl(value)) {
                    collector.addVideoUrl(value);
                }
                if (isSubtitleUrlField(key)) {
                    collector.addSubtitleUrl(value);
                }
                if (isImageTextField(key)) {
                    collector.addImageText(value);
                }
                if (isVideoTextField(key)) {
                    collector.addVideoText(value);
                }
            }
        }
        extractUrlsFromText(html, collector);
        if (expensiveExtraction && properties.isImageOcrEnabled()) {
            collectImageOcr(collector, false);
        }
        if (expensiveExtraction && properties.isVideoAsrEnabled()) {
            collectVideoAsr(collector);
        }
        return collector.toInsights();
    }

    private Map<String, String> parseAttributes(String rawAttrs) {
        Map<String, String> attrs = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawAttrs)) return attrs;
        Matcher matcher = ATTR_PATTERN.matcher(rawAttrs);
        while (matcher.find()) {
            String value = matcher.group(2);
            if (value == null) value = matcher.group(3);
            if (value == null) value = matcher.group(4);
            attrs.put(matcher.group(1), value == null ? "" : value);
        }
        return attrs;
    }

    private void extractUrlsFromText(String raw, MediaCollector collector) {
        if (!StringUtils.hasText(raw)) return;
        Matcher matcher = URL_PATTERN.matcher(raw);
        while (matcher.find()) {
            String url = trimUrlTail(unescapeHtml(matcher.group()));
            if (looksLikeImageUrl(url)) {
                collector.addImageUrl(url);
            } else if (looksLikeVideoUrl(url)) {
                collector.addVideoUrl(url);
            }
        }
    }

    private String fetchSubtitleText(String subtitleUrl) {
        try {
            byte[] bytes = fetchBytes(subtitleUrl, properties.getMediaTextMaxLength() * 4, MediaType.TEXT_PLAIN);
            if (bytes.length == 0) return "";
            String raw = new String(bytes, StandardCharsets.UTF_8);
            return normalizeSubtitleText(raw);
        } catch (Exception e) {
            log.debug("抖音学习中心视频字幕下载失败 subtitle={}, err={}", subtitleUrl, e.getMessage());
            return "";
        }
    }

    private String normalizeSubtitleText(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        String text = raw.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n');
        if (looksLikeJson(text)) {
            try {
                text = extractTextFromJson(objectMapper.readTree(text), 0);
            } catch (Exception ignored) {
            }
        }
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (String line : text.split("\n")) {
            String cleaned = extractReadableText(line);
            if (!StringUtils.hasText(cleaned)) continue;
            if ("WEBVTT".equalsIgnoreCase(cleaned)) continue;
            if (SUBTITLE_SEQUENCE_PATTERN.matcher(cleaned).matches()) continue;
            if (SUBTITLE_TIMESTAMP_PATTERN.matcher(cleaned).matches()) continue;
            if (cleaned.startsWith("NOTE ")) continue;
            lines.add(cleaned);
        }
        return truncate(String.join(" ", lines), properties.getMediaTextMaxLength());
    }

    private boolean isUsefulMediaExtractionText(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String text = clean(raw);
        if (!StringUtils.hasText(text) || looksLikeUrlOnly(text)) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("image too small")
                || lower.contains("empty page")
                || lower.contains("error in pix")
                || lower.contains("tesseract open source ocr engine")) {
            return false;
        }
        return effectiveContentLength(text) >= 4;
    }

    private String extractTextFromJson(JsonNode node, int depth) {
        if (node == null || node.isMissingNode() || depth > 8) return "";
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        collectTextFromJson(node, texts, depth, "");
        return truncate(String.join("\n", texts), properties.getMediaTextMaxLength());
    }

    private void collectTextFromJson(JsonNode node, LinkedHashSet<String> texts, int depth, String fieldName) {
        if (node == null || node.isMissingNode() || depth > 8 || texts.size() > 300) return;
        if (node.isTextual()) {
            String value = unescapeHtml(node.asText());
            if (!StringUtils.hasText(value)) return;
            if (looksLikeJson(value)) {
                try {
                    collectTextFromJson(objectMapper.readTree(value), texts, depth + 1, fieldName);
                    return;
                } catch (Exception ignored) {
                }
            }
            String normalizedField = normalizeForMatch(fieldName);
            if (isUsefulTextField(normalizedField) || looksLikePlainContent(value)) {
                String text = extractReadableText(value);
                if (effectiveContentLength(text) >= 2) {
                    texts.add(text);
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) collectTextFromJson(child, texts, depth + 1, fieldName);
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> collectTextFromJson(e.getValue(), texts, depth + 1, e.getKey()));
        }
    }

    private String extractReadableText(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        String text = raw;
        if (text.contains("<") && text.contains(">")) {
            text = TAG_PATTERN.matcher(text).replaceAll(" ");
        }
        text = unescapeHtml(text);
        text = SPACE_PATTERN.matcher(text).replaceAll(" ").trim();
        if (looksLikeUrlOnly(text)) return "";
        return truncate(text, properties.getMediaTextMaxLength());
    }

    private List<String> mergeTags(List<String> baseTags, JsonNode... nodes) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (baseTags != null) tags.addAll(baseTags);
        for (JsonNode node : nodes) {
            collectTagTexts(node, tags);
        }
        return tags.stream().filter(StringUtils::hasText).distinct().toList();
    }

    private void collectTagTexts(JsonNode node, LinkedHashSet<String> tags) {
        if (node == null || node.isMissingNode()) return;
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            tags.add(clean(node.asText()));
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) collectTagTexts(child, tags);
            return;
        }
        if (node.isObject()) {
            String text = firstText(node, "name", "title", "tag_name");
            if (StringUtils.hasText(text)) tags.add(clean(text));
        }
    }

    private void collectImageOcr(MediaCollector collector, boolean violation) {
        if (!properties.isImageOcrEnabled() || !StringUtils.hasText(properties.getImageOcrCommand())) return;
        int max = violation
                ? Math.max(0, properties.getMaxImagesPerItem())
                : Math.max(0, properties.getMaxImageOcrPerItem());
        int processed = 0;
        for (String imageUrl : collector.imageUrlsSnapshot()) {
            if (processed >= max) return;
            processed++;
            String text = runImageOcr(imageUrl);
            if (isUsefulMediaExtractionText(text)) {
                collector.addImageText("OCR " + imageUrl + "：" + text);
                collector.addMediaStatus("图片 OCR 成功：" + imageUrl);
            } else {
                collector.addMediaStatus("图片 OCR 无有效文字：" + imageUrl);
            }
        }
    }

    private String runImageOcr(String imageUrl) {
        Path imageFile = null;
        try {
            MediaFetchResult image = fetchMedia(imageUrl, properties.getMaxImageBytes(), MediaType.IMAGE_JPEG);
            byte[] imageBytes = image.body();
            if (imageBytes.length == 0) return "";
            String suffix = imageSuffix(imageUrl);
            imageFile = Files.createTempFile("douyin-school-ocr-", suffix);
            Files.write(imageFile, imageBytes);
            List<String> command = buildExternalCommand(properties.getImageOcrCommand(), imageFile.toString(), imageUrl);
            String output = runCommand(command, properties.getImageOcrTimeoutSeconds());
            return clean(output);
        } catch (Exception e) {
            log.debug("抖音学习中心图片 OCR 失败 image={}, err={}", imageUrl, e.getMessage());
            return "";
        } finally {
            if (imageFile != null) {
                try {
                    Files.deleteIfExists(imageFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void collectVideoAsr(MediaCollector collector) {
        if (!properties.isVideoAsrEnabled() || !hasVideoAsrTarget()) return;
        int max = Math.max(0, properties.getMaxVideoAsrPerItem());
        int processed = 0;
        for (String videoUrl : collector.videoUrlsSnapshot()) {
            if (processed >= max) return;
            processed++;
            MediaExtractionResult result = runVideoAsr(videoUrl);
            String text = result.text();
            if (isUsefulMediaExtractionText(text)) {
                collector.addVideoText("ASR " + videoUrl + "：" + text);
                collector.addMediaStatus("视频 ASR 成功：" + videoUrl);
            } else {
                collector.addMediaStatus(StringUtils.hasText(result.status())
                        ? result.status()
                        : "视频 ASR 无有效字幕：" + videoUrl);
            }
        }
    }

    private boolean hasVideoAsrTarget() {
        return StringUtils.hasText(properties.getVideoAsrCommand())
                || StringUtils.hasText(properties.getVideoAsrApiUrl());
    }

    private MediaExtractionResult runVideoAsr(String videoUrl) {
        Path videoFile = null;
        try {
            MediaFetchResult media = fetchMedia(videoUrl, properties.getMaxVideoBytes(), MediaType.valueOf("video/mp4"));
            byte[] videoBytes = media.body();
            boolean directMedia = isLikelyAudioVideo(media, videoUrl);
            if (!directMedia && !StringUtils.hasText(properties.getVideoAsrApiUrl())) {
                log.info("抖音学习中心视频 ASR 跳过非直连媒体 video={}, contentType={}, bytes={}",
                        videoUrl, media.contentType(), videoBytes.length);
                return new MediaExtractionResult("", "视频 ASR 跳过非直连媒体：" + videoUrl + "，contentType=" + media.contentType());
            }
            String output;
            if (StringUtils.hasText(properties.getVideoAsrApiUrl())) {
                output = directMedia
                        ? callVideoAsrApi(videoUrl, videoBytes)
                        : callVideoAsrApiForUrl(videoUrl);
            } else {
                if (videoBytes.length == 0) {
                    return new MediaExtractionResult("", "视频 ASR 下载为空：" + videoUrl);
                }
                videoFile = Files.createTempFile("douyin-school-video-", videoSuffix(videoUrl));
                Files.write(videoFile, videoBytes);
                List<String> command = buildExternalCommand(properties.getVideoAsrCommand(), videoFile.toString(), videoUrl);
                output = runCommand(command, properties.getVideoAsrTimeoutSeconds());
            }
            return new MediaExtractionResult(clean(output), "");
        } catch (Exception e) {
            String reason = truncate(clean(e.getMessage()), 320);
            log.info("抖音学习中心视频 ASR 失败 video={}, err={}", videoUrl, reason);
            return new MediaExtractionResult("", "视频 ASR 失败：" + videoUrl + "，原因：" + reason);
        } finally {
            if (videoFile != null) {
                try {
                    Files.deleteIfExists(videoFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private byte[] fetchBytes(String url, int maxBytes) {
        return fetchBytes(url, maxBytes, MediaType.APPLICATION_OCTET_STREAM);
    }

    private byte[] fetchBytes(String url, int maxBytes, MediaType preferredAccept) {
        return fetchMedia(url, maxBytes, preferredAccept).body();
    }

    private MediaFetchResult fetchMedia(String url, int maxBytes, MediaType preferredAccept) {
        if (!StringUtils.hasText(url) || maxBytes <= 0) return MediaFetchResult.empty();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(preferredAccept, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36");
        ResponseEntity<byte[]> response = requestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        byte[] body = response.getBody();
        String contentType = response.getHeaders().getContentType() == null ? "" : response.getHeaders().getContentType().toString();
        String finalUrl = response.getHeaders().getLocation() == null ? url : response.getHeaders().getLocation().toString();
        if (body == null || body.length == 0 || body.length > maxBytes) {
            return new MediaFetchResult(new byte[0], contentType, finalUrl);
        }
        return new MediaFetchResult(body, contentType, finalUrl);
    }

    private boolean isLikelyAudioVideo(MediaFetchResult media, String url) {
        if (media == null || media.body().length == 0) return false;
        String contentType = media.contentType() == null ? "" : media.contentType().toLowerCase(Locale.ROOT);
        if (contentType.startsWith("video/") || contentType.startsWith("audio/")
                || contentType.contains("mpegurl") || contentType.contains("mp2t")
                || contentType.contains("octet-stream")) {
            return !looksLikeHtml(media.body()) && !looksLikeImageBytes(media.body());
        }
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m4a") || lowerUrl.contains(".wav")
                || lowerUrl.contains(".webm") || lowerUrl.contains(".mp3") || lowerUrl.contains(".m3u8")) {
            return !looksLikeHtml(media.body()) && !looksLikeImageBytes(media.body());
        }
        return false;
    }

    private static boolean looksLikeHtml(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return false;
        String prefix = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT);
        return prefix.startsWith("<!doctype html") || prefix.startsWith("<html") || prefix.contains("<html");
    }

    private static boolean looksLikeImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        boolean jpeg = bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
        boolean png = bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
        boolean webp = bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return jpeg || png || webp;
    }

    private String callVideoAsrApi(String videoUrl, byte[] videoBytes) {
        if (!StringUtils.hasText(properties.getVideoAsrApiUrl())) return "";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        if (StringUtils.hasText(properties.getVideoAsrApiKey())) {
            headers.setBearerAuth(properties.getVideoAsrApiKey());
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", StringUtils.hasText(properties.getVideoAsrModel()) ? properties.getVideoAsrModel() : "whisper-1");
        body.add("language", "zh");
        body.add("response_format", "json");
        body.add("file", new ByteArrayResource(videoBytes) {
            @Override
            public String getFilename() {
                return "douyin-school-video" + videoSuffix(videoUrl);
            }
        });

        return postVideoAsrApi(headers, body);
    }

    private String callVideoAsrApiForUrl(String videoUrl) {
        if (!StringUtils.hasText(properties.getVideoAsrApiUrl()) || !StringUtils.hasText(videoUrl)) return "";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        if (StringUtils.hasText(properties.getVideoAsrApiKey())) {
            headers.setBearerAuth(properties.getVideoAsrApiKey());
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", StringUtils.hasText(properties.getVideoAsrModel()) ? properties.getVideoAsrModel() : "whisper-1");
        body.add("language", "zh");
        body.add("response_format", "json");
        body.add("url", videoUrl);
        return postVideoAsrApi(headers, body);
    }

    private String postVideoAsrApi(HttpHeaders headers, MultiValueMap<String, Object> body) {
        ResponseEntity<String> response = videoAsrTemplate().exchange(
                properties.getVideoAsrApiUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        return extractAsrText(response.getBody());
    }

    private String extractAsrText(String body) {
        if (!StringUtils.hasText(body)) return "";
        String trimmed = body.trim();
        if (!looksLikeJson(trimmed)) return trimmed;
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            String text = firstText(root, "text", "transcript", "content");
            if (StringUtils.hasText(text)) return text;
            return extractTextFromJson(root, 0);
        } catch (Exception e) {
            return trimmed;
        }
    }

    private List<String> buildExternalCommand(String template, String inputPath, String url) {
        String resolved = template
                .replace("{input}", inputPath == null ? "" : inputPath)
                .replace("{url}", url == null ? "" : url);
        return Arrays.stream(resolved.split("\\s+")).filter(StringUtils::hasText).toList();
    }

    private String runCommand(List<String> command, int timeoutSeconds) throws Exception {
        if (command == null || command.isEmpty()) return "";
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return "";
        }
        byte[] output = process.getInputStream().readAllBytes();
        if (process.exitValue() != 0) return "";
        return new String(output, StandardCharsets.UTF_8);
    }

    private String imageSuffix(String imageUrl) {
        String lower = imageUrl == null ? "" : imageUrl.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".webp")) return ".webp";
        return ".jpg";
    }

    private String videoSuffix(String videoUrl) {
        String lower = videoUrl == null ? "" : videoUrl.toLowerCase(Locale.ROOT);
        if (lower.contains(".m4a")) return ".m4a";
        if (lower.contains(".wav")) return ".wav";
        if (lower.contains(".webm")) return ".webm";
        return ".mp4";
    }

    private static String mergeContent(String... parts) {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        if (parts != null) {
            for (String part : parts) {
                String text = clean(part);
                if (StringUtils.hasText(text)) {
                    texts.add(text);
                }
            }
        }
        return String.join("\n", texts);
    }

    private static void appendSection(StringBuilder sb, String title, List<String> lines) {
        if (lines == null || lines.isEmpty()) return;
        sb.append("\n## ").append(title).append("\n\n");
        for (String line : lines) {
            if (StringUtils.hasText(line)) {
                sb.append("- ").append(clean(line)).append("\n");
            }
        }
    }

    private List<String> extractLinks(String html, String baseUrl) {
        LinkedHashSet<String> links = new LinkedHashSet<>();
        if (!StringUtils.hasText(html)) return List.of();
        Matcher linkMatcher = LINK_PATTERN.matcher(html);
        while (linkMatcher.find()) {
            String url = normalizeUrl(resolveUrl(linkMatcher.group(1), baseUrl));
            if (StringUtils.hasText(url)) links.add(url);
        }
        Matcher routeMatcher = ROUTE_PATTERN.matcher(html);
        while (routeMatcher.find()) {
            String url = normalizeUrl(normalizeBaseUrl() + routeMatcher.group(1));
            if (StringUtils.hasText(url)) links.add(url);
        }
        return links.stream().filter(this::isOfficialUrl).toList();
    }

    private String fetch(String url, MediaType accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(accept, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.ALL));
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124 Safari/537.36");
        headers.set(HttpHeaders.REFERER, normalizeBaseUrl() + "/doudian/web/home");
        headers.set("x-eschool-source", "");
        headers.set("x-eschool-referrer", "");
        ResponseEntity<String> response = requestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody() != null ? response.getBody() : "";
    }

    private RestTemplate videoAsrTemplate() {
        int timeoutMs = Math.max(1, properties.getVideoAsrTimeoutSeconds()) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(5_000, Math.min(timeoutMs, 30_000)));
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private RestTemplate requestTemplate() {
        int timeoutMs = Math.max(1, properties.getHttpTimeoutSeconds()) * 1000;
        try {
            if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory factory) {
                factory.setConnectTimeout(timeoutMs);
                factory.setReadTimeout(timeoutMs);
                return restTemplate;
            }
        } catch (Exception ignored) {
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    private boolean shouldLogProgress(int processed) {
        int every = Math.max(1, properties.getProgressLogEvery());
        return processed == 1 || processed % every == 0;
    }

    private String extractTitle(String html) {
        if (!StringUtils.hasText(html)) return "";
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            return clean(unescapeHtml(matcher.group(1)));
        }
        return "";
    }

    private String extractText(String html) {
        if (!StringUtils.hasText(html)) return "";
        String cleaned = SCRIPT_PATTERN.matcher(html).replaceAll(" ");
        cleaned = STYLE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = unescapeHtml(cleaned);
        cleaned = SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.length() > 12_000) {
            cleaned = cleaned.substring(0, 12_000);
        }
        return cleaned;
    }

    private String resolveUrl(String raw, String baseUrl) {
        if (!StringUtils.hasText(raw)) return "";
        String url = raw.trim();
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("/")) return normalizeBaseUrl() + url;
        try {
            return URI.create(baseUrl).resolve(url).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) return "";
        String normalized = url.trim();
        if (normalized.startsWith("//")) normalized = "https:" + normalized;
        if (normalized.contains("#")) normalized = normalized.substring(0, normalized.indexOf('#'));
        int query = normalized.indexOf('?');
        if (query >= 0) {
            normalized = normalized.substring(0, query);
        }
        try {
            normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return normalized;
    }

    private String normalizeMediaUrl(String url, String baseUrl) {
        if (!StringUtils.hasText(url)) return "";
        String normalized = trimUrlTail(url.trim());
        if (normalized.startsWith("//")) normalized = "https:" + normalized;
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) return normalized;
        return resolveUrl(normalized, StringUtils.hasText(baseUrl) ? baseUrl : normalizeBaseUrl());
    }

    private boolean isOfficialUrl(String url) {
        return StringUtils.hasText(url)
                && (url.startsWith(normalizeBaseUrl()) || url.startsWith("https://school.jinritemai.com"));
    }

    private boolean isOfficialContentUrl(String url) {
        if (!isOfficialUrl(url)) return false;
        return url.contains("/doudian/web/article/")
                || url.contains("/doudian/web/video-article/")
                || url.contains("/doudian/web/course-series/")
                || url.contains("/doudian/web/topic/");
    }

    private boolean isSeedLikeUrl(String url) {
        if (!isOfficialUrl(url)) return false;
        return url.endsWith("/doudian/web/rules")
                || url.endsWith("/doudian/web/funcs")
                || url.endsWith("/doudian/web/ecomcase")
                || url.contains("/doudian/web/help");
    }

    private String normalizeBaseUrl() {
        String base = properties.getBaseUrl();
        if (!StringUtils.hasText(base)) return "https://school.jinritemai.com";
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String titleFromUrl(String url) {
        if (!StringUtils.hasText(url)) return "抖音电商学习中心官方资料";
        String normalized = normalizeUrl(url);
        int idx = normalized.lastIndexOf('/');
        String tail = idx >= 0 ? normalized.substring(idx + 1) : normalized;
        if (!StringUtils.hasText(tail)) return "抖音电商学习中心官方资料";
        return "抖音电商学习中心官方资料 " + tail;
    }

    private String idFromUrl(String url) {
        if (!StringUtils.hasText(url)) return "";
        String normalized = normalizeUrl(url);
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : "";
    }

    private int objTypeFromUrl(String url) {
        if (!StringUtils.hasText(url)) return 0;
        if (url.contains("/video-article/")) return 15;
        if (url.contains("/course-series/")) return 3;
        return 1;
    }

    private String categoryFromUrl(String url) {
        if (!StringUtils.hasText(url)) return "官方资料";
        if (url.contains("/rules")) return "规则中心";
        if (url.contains("/funcs")) return "功能中心";
        if (url.contains("/ecomcase")) return "案例中心";
        if (url.contains("/video-article/")) return "短视频";
        if (url.contains("/course-series/")) return "课程";
        return "官方资料";
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isTextual() && StringUtils.hasText(value.asText())) return value.asText();
            if (value.isNumber()) return value.asText();
        }
        return "";
    }

    private static boolean containsAny(String raw, List<String> keywords) {
        String text = normalizeForMatch(raw);
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(normalizeForMatch(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeForMatch(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static boolean looksLikeJson(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String trimmed = raw.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private static boolean looksLikeImageUrl(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String text = raw.toLowerCase(Locale.ROOT);
        return text.contains(".jpg") || text.contains(".jpeg") || text.contains(".png") || text.contains(".webp")
                || text.contains("image") || text.contains("douyinpic") || text.contains("ecombdimg");
    }

    private static boolean looksLikeVideoUrl(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String text = raw.toLowerCase(Locale.ROOT);
        return text.contains(".mp4") || text.contains(".m3u8") || text.contains("mime_type=video")
                || text.contains("ecombdvod") || text.contains("video/tos") || text.contains("live.byte")
                || text.contains("live.livesaas") || text.contains("live_url");
    }

    private static boolean isImageField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return IMAGE_URL_FIELDS.stream().map(DouyinSchoolCollectorServiceImpl::normalizeForMatch)
                .anyMatch(fieldName::contains);
    }

    private static boolean isVideoField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return VIDEO_URL_FIELDS.stream().map(DouyinSchoolCollectorServiceImpl::normalizeForMatch)
                .anyMatch(fieldName::contains);
    }

    private static boolean isSubtitleUrlField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return SUBTITLE_URL_FIELDS.stream().map(DouyinSchoolCollectorServiceImpl::normalizeForMatch)
                .anyMatch(fieldName::contains);
    }

    private static boolean isUsefulTextField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return DETAIL_TEXT_FIELDS.stream().map(DouyinSchoolCollectorServiceImpl::normalizeForMatch)
                .anyMatch(fieldName::contains);
    }

    private static boolean isImageTextField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return fieldName.contains("alt") || fieldName.contains("title")
                || fieldName.contains("caption") || fieldName.contains("aria-label");
    }

    private static boolean isVideoTextField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) return false;
        return fieldName.contains("subtitle") || fieldName.contains("transcript") || fieldName.contains("caption")
                || fieldName.contains("videotext") || fieldName.contains("plain_description")
                || fieldName.contains("format_description") || fieldName.contains("asrtext")
                || fieldName.contains("speechtext") || fieldName.contains("voicetext")
                || fieldName.contains("dialogue") || fieldName.contains("script");
    }

    private static boolean looksLikePlainContent(String raw) {
        if (!StringUtils.hasText(raw) || looksLikeUrlOnly(raw)) return false;
        return effectiveContentLength(raw) >= 6
                && (raw.matches(".*[\\u4e00-\\u9fa5].*") || raw.contains("违规") || raw.contains("规则"));
    }

    private static boolean isTruthy(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return false;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNumber()) return node.asInt() != 0;
        if (node.isTextual()) {
            String text = normalizeForMatch(node.asText());
            return "true".equals(text) || "1".equals(text) || "yes".equals(text);
        }
        return false;
    }

    private static boolean looksLikeUrlOnly(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String text = raw.trim().toLowerCase(Locale.ROOT);
        return text.startsWith("http://") || text.startsWith("https://") || text.startsWith("//");
    }

    private static boolean looksLikeUrlReference(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        String text = raw.trim().toLowerCase(Locale.ROOT);
        return text.startsWith("http://") || text.startsWith("https://") || text.startsWith("//") || text.startsWith("/");
    }

    private static String trimUrlTail(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        while (text.endsWith(",") || text.endsWith(".") || text.endsWith(";") || text.endsWith(")") || text.endsWith("]")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String clean(String raw) {
        if (raw == null) return "";
        return SPACE_PATTERN.matcher(unescapeHtml(raw)).replaceAll(" ").trim();
    }

    private static int effectiveContentLength(String raw) {
        if (raw == null) return 0;
        return raw.replaceAll("[#\\-：:、，。,.\\s]", "").length();
    }

    private static boolean isNoIndexableOrDuplicate(Exception e) {
        String msg = e != null && e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("无可索引分块") || msg.contains("chunk 级去重全部被跳过");
    }

    private static boolean isNewlyCreated(AiKbDocument doc, long uploadStartedAt) {
        if (doc == null || doc.getCreateTime() == null) return false;
        return doc.getCreateTime().getTime() >= uploadStartedAt - 1_000;
    }

    private static String unescapeHtml(String raw) {
        if (raw == null) return "";
        return raw.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String truncate(String raw, int max) {
        if (raw == null) return "";
        return raw.length() <= max ? raw : raw.substring(0, max);
    }

    private static String encodeParam(String raw) {
        if (raw == null) return "";
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    private static void sleepQuietly(int ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record OfficialItem(
            String title,
            String content,
            String url,
            String sourceId,
            int objType,
            String category,
            List<String> tags,
            long updateTimestamp,
            long createTimestamp,
            List<String> imageTexts,
            List<String> videoTexts,
            List<String> imageUrls,
            List<String> videoUrls,
            List<String> mediaExtractionStatuses
    ) {
        private OfficialItem {
            tags = tags == null ? List.of() : tags.stream().filter(Objects::nonNull).distinct().toList();
            imageTexts = sanitizeList(imageTexts, 40);
            videoTexts = sanitizeList(videoTexts, 40);
            imageUrls = sanitizeList(imageUrls, 30);
            videoUrls = sanitizeList(videoUrls, 20);
            mediaExtractionStatuses = sanitizeList(mediaExtractionStatuses, 40);
        }

        private OfficialItem merge(String newTitle, String newContent, MediaInsights insights) {
            return new OfficialItem(
                    StringUtils.hasText(newTitle) ? newTitle : title,
                    StringUtils.hasText(newContent) ? newContent : content,
                    url,
                    sourceId,
                    objType,
                    category,
                    tags,
                    updateTimestamp,
                    createTimestamp,
                    mergeLists(imageTexts, insights == null ? List.of() : insights.imageTexts(), 40),
                    mergeLists(videoTexts, insights == null ? List.of() : insights.videoTexts(), 40),
                    mergeLists(imageUrls, insights == null ? List.of() : insights.imageUrls(), 30),
                    mergeLists(videoUrls, insights == null ? List.of() : insights.videoUrls(), 20),
                    mergeLists(mediaExtractionStatuses, insights == null ? List.of() : insights.mediaExtractionStatuses(), 40)
            );
        }

        private String combinedSearchText() {
            return String.join("\n", mergeLists(List.of(content), mergeLists(imageTexts, videoTexts, 80), 100));
        }
    }

    private record MediaInsights(
            List<String> imageTexts,
            List<String> videoTexts,
            List<String> imageUrls,
            List<String> videoUrls,
            List<String> mediaExtractionStatuses
    ) {
        private MediaInsights {
            imageTexts = sanitizeList(imageTexts, 40);
            videoTexts = sanitizeList(videoTexts, 40);
            imageUrls = sanitizeList(imageUrls, 30);
            videoUrls = sanitizeList(videoUrls, 20);
            mediaExtractionStatuses = sanitizeList(mediaExtractionStatuses, 40);
        }
    }

    private record MediaFetchResult(byte[] body, String contentType, String finalUrl) {
        private MediaFetchResult {
            body = body == null ? new byte[0] : body;
            contentType = contentType == null ? "" : contentType;
            finalUrl = finalUrl == null ? "" : finalUrl;
        }

        private static MediaFetchResult empty() {
            return new MediaFetchResult(new byte[0], "", "");
        }
    }

    private record MediaExtractionResult(String text, String status) {
    }

    private class MediaCollector {
        private final String baseUrl;
        private final LinkedHashSet<String> contentTexts = new LinkedHashSet<>();
        private final LinkedHashSet<String> imageTexts = new LinkedHashSet<>();
        private final LinkedHashSet<String> videoTexts = new LinkedHashSet<>();
        private final LinkedHashSet<String> imageUrls = new LinkedHashSet<>();
        private final LinkedHashSet<String> videoUrls = new LinkedHashSet<>();
        private final LinkedHashSet<String> mediaExtractionStatuses = new LinkedHashSet<>();
        private final LinkedHashSet<String> subtitleUrls = new LinkedHashSet<>();

        private MediaCollector(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        private void addContentText(String text) {
            addText(contentTexts, text);
        }

        private void addImageText(String text) {
            addText(imageTexts, text);
        }

        private void addVideoText(String text) {
            addText(videoTexts, text);
        }

        private void addMediaStatus(String text) {
            addText(mediaExtractionStatuses, text);
        }

        private void addImageUrl(String url) {
            if (!properties.isExtractImages()) return;
            String normalized = normalizeMediaUrl(url, baseUrl);
            if (StringUtils.hasText(normalized) && looksLikeImageUrl(normalized)
                    && imageUrls.size() < Math.max(0, properties.getMaxImagesPerItem())) {
                imageUrls.add(normalized);
            }
        }

        private void addVideoUrl(String url) {
            if (!properties.isExtractVideos()) return;
            String normalized = normalizeMediaUrl(url, baseUrl);
            if (StringUtils.hasText(normalized) && looksLikeVideoUrl(normalized)
                    && videoUrls.size() < Math.max(0, properties.getMaxVideosPerItem())) {
                videoUrls.add(normalized);
            }
        }

        private void addSubtitleUrl(String url) {
            if (!properties.isExtractVideos()) return;
            String normalized = normalizeMediaUrl(url, baseUrl);
            if (!StringUtils.hasText(normalized) || looksLikeVideoUrl(normalized) || looksLikeImageUrl(normalized)) {
                return;
            }
            if (subtitleUrls.size() >= Math.max(0, properties.getMaxVideosPerItem())) return;
            if (subtitleUrls.contains(normalized)) return;
            String text = fetchSubtitleText(normalized);
            if (isUsefulMediaExtractionText(text)) {
                subtitleUrls.add(normalized);
                addVideoText("官方字幕 " + normalized + "：" + text);
            }
        }

        private List<String> imageUrlsSnapshot() {
            return List.copyOf(imageUrls);
        }

        private List<String> videoUrlsSnapshot() {
            return List.copyOf(videoUrls);
        }

        private MediaInsights toInsights() {
            return new MediaInsights(
                    List.copyOf(imageTexts),
                    List.copyOf(videoTexts),
                    List.copyOf(imageUrls),
                    List.copyOf(videoUrls),
                    List.copyOf(mediaExtractionStatuses)
            );
        }

        private void addText(LinkedHashSet<String> target, String text) {
            String cleaned = clean(text);
            if (!StringUtils.hasText(cleaned) || looksLikeUrlOnly(cleaned)) return;
            target.add(truncate(cleaned, properties.getMediaTextMaxLength()));
        }
    }

    private static List<String> mergeLists(List<String> first, List<String> second, int max) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) merged.addAll(first);
        if (second != null) merged.addAll(second);
        return sanitizeList(List.copyOf(merged), max);
    }

    private static List<String> sanitizeList(List<String> values, int max) {
        if (values == null || max <= 0) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(DouyinSchoolCollectorServiceImpl::clean)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(max)
                .toList();
    }

    private record CollectorProgress(
            boolean running,
            String stage,
            String startedAt,
            String updatedAt,
            int discovered,
            int processed,
            int importedGeneral,
            int importedViolation,
            int existing,
            int skipped,
            int failed,
            String currentTitle,
            String currentUrl,
            String message
    ) {
        private static CollectorProgress idle() {
            return new CollectorProgress(false, "idle", "", Instant.now().toString(),
                    0, 0, 0, 0, 0, 0, 0, "", "", "暂无采集任务");
        }

        private static CollectorProgress running(String stage, Instant startedAt, int discovered, int processed,
                                                 int importedGeneral, int importedViolation, int existing,
                                                 int skipped, int failed, String currentTitle,
                                                 String currentUrl, String message) {
            return new CollectorProgress(true, stage, startedAt.toString(), Instant.now().toString(),
                    discovered, processed, importedGeneral, importedViolation, existing, skipped, failed,
                    currentTitle == null ? "" : currentTitle,
                    currentUrl == null ? "" : currentUrl,
                    message == null ? "" : message);
        }

        private static CollectorProgress finished(String stage, Instant startedAt, int discovered, int processed,
                                                  int importedGeneral, int importedViolation, int existing,
                                                  int skipped, int failed, String message) {
            return new CollectorProgress(false, stage, startedAt.toString(), Instant.now().toString(),
                    discovered, processed, importedGeneral, importedViolation, existing, skipped, failed,
                    "", "", message == null ? "" : message);
        }

        private Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("running", running);
            m.put("stage", stage);
            m.put("startedAt", startedAt);
            m.put("updatedAt", updatedAt);
            m.put("discovered", discovered);
            m.put("processed", processed);
            m.put("progressPercent", discovered <= 0 ? 0 : Math.min(100, Math.round(processed * 100.0 / discovered)));
            m.put("importedGeneral", importedGeneral);
            m.put("importedViolation", importedViolation);
            m.put("existing", existing);
            m.put("skipped", skipped);
            m.put("failed", failed);
            m.put("currentTitle", currentTitle);
            m.put("currentUrl", currentUrl);
            m.put("message", message);
            return m;
        }
    }

    private record TopicProfile(String code, String name, String description, List<String> keywords) {
    }

    private enum MediaKind {
        IMAGE,
        VIDEO,
        UNKNOWN
    }
}
