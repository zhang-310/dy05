package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDailyBatchRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyContentService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiCopyGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 短视频一键日更批量生成服务实现
 * 集成知识库 RAG 检索，为每个热点话题检索相关知识库素材，丰富生成上下文
 */
@Service
public class DailyContentServiceImpl implements DailyContentService {

    private static final Logger log = LoggerFactory.getLogger(DailyContentServiceImpl.class);

    @Resource
    private SvDailyBatchRepository dailyBatchRepository;

    @Resource
    private SvHotTopicRepository hotTopicRepository;

    @Resource
    private SvViralVideoRepository svViralVideoRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private ShortVideoAiService shortVideoAiService;

    @Autowired(required = false)
    private ContentCalendarService contentCalendarService;

    @Override
    @Transactional
    public SvDailyBatch generateBatch(Long ownerId, Long personaId, String sourceType, int batchSize) {
        if (batchSize < 1 || batchSize > 10) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "batchSize 需在 1-10 之间");
        }

        // 1. 创建批次记录
        SvDailyBatch batch = new SvDailyBatch();
        batch.setOwnerId(ownerId);
        batch.setPersonaId(personaId);
        batch.setSourceType(sourceType);
        batch.setBatchSize(batchSize);
        batch.setStatus("processing");
        dailyBatchRepository.save(batch);

        log.info("创建日更批次 {}，用户 {}，来源 {}，数量 {}", batch.getId(), ownerId, sourceType, batchSize);

        try {
            List<String> topics = resolveTopics(sourceType, ownerId, batchSize);

            List<Map<String, Object>> enrichedTopics = new ArrayList<>();
            for (String topic : topics) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("topic", topic);

                List<Map<String, String>> ragRefs = retrieveKnowledgeForTopic(topic, ownerId);
                if (!ragRefs.isEmpty()) {
                    item.put("knowledgeRefs", ragRefs);
                }

                if (shortVideoAiService != null) {
                    try {
                        AiCopyGenerateVO copyVo = new AiCopyGenerateVO();
                        copyVo.setTopic(topic);
                        copyVo.setPersonaId(personaId);
                        String aiCopy = shortVideoAiService.generateCopy(copyVo, ownerId);
                        item.put("aiCopy", aiCopy);
                    } catch (Exception e) {
                        log.warn("日更批次 AI 文案生成失败: topic={}, error={}", topic, e.getMessage());
                    }
                }

                enrichedTopics.add(item);
            }

            // 自动排期到内容日历
            if (contentCalendarService != null) {
                try {
                    for (int i = 0; i < enrichedTopics.size(); i++) {
                        Map<String, Object> topicItem = enrichedTopics.get(i);
                        String topicTitle = (String) topicItem.get("topic");
                        java.time.LocalDate scheduleDate = java.time.LocalDate.now().plusDays(i + 1);
                        contentCalendarService.quickSchedule(ownerId, topicTitle, scheduleDate);
                    }
                    log.info("日更批次 {} 已自动排期 {} 条到内容日历", batch.getId(), enrichedTopics.size());
                } catch (Exception e) {
                    log.warn("日更批次 {} 自动排期失败: {}", batch.getId(), e.getMessage());
                }
            }

            String summary = objectMapper.writeValueAsString(Map.of(
                    "topics", enrichedTopics,
                    "count", enrichedTopics.size(),
                    "ragEnabled", knowledgeBaseService != null,
                    "aiCopyEnabled", shortVideoAiService != null
            ));
            batch.setResultSummary(summary);
            batch.setStatus("completed");
            dailyBatchRepository.save(batch);

            log.info("日更批次 {} 已完成，共 {} 条话题，RAG={}", batch.getId(), topics.size(), knowledgeBaseService != null);
        } catch (Exception e) {
            log.error("日更批次 {} 处理失败", batch.getId(), e);
            batch.setStatus("failed");
            dailyBatchRepository.save(batch);
        }

        return batch;
    }

    @Override
    public SvDailyBatch getBatchStatus(Long batchId) {
        return dailyBatchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "批次不存在"));
    }

    @Override
    public PageResultVO<SvDailyBatch> listBatches(Long ownerId, int page, int rows) {
        if (rows < 1) rows = 10;
        if (rows > 100) rows = 100;
        var pg = dailyBatchRepository.findByOwnerIdAndDeleted(
                ownerId, 0, PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResultVO.of(pg.getTotalElements(), pg.getContent(), page, rows);
    }

    private List<String> getHotTopics(int limit) {
        List<SvHotTopic> topics = hotTopicRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "heatScore"))
        ).getContent();

        if (topics.isEmpty()) {
            return List.of("暂无热点话题");
        }

        return topics.stream()
                .map(SvHotTopic::getTitle)
                .toList();
    }

    private List<String> resolveTopics(String sourceType, Long ownerId, int limit) {
        if ("hot_topic".equals(sourceType)) {
            return dedupeTopics(getHotTopics(limit), limit);
        }
        if ("viral_video".equals(sourceType)) {
            List<String> viralTopics = getViralVideoTopics(ownerId, limit);
            if (!viralTopics.isEmpty()) {
                return dedupeTopics(viralTopics, limit);
            }
        }

        List<String> fallback = new ArrayList<>();
        fallback.addAll(getHotTopics(limit));
        fallback.addAll(getKnowledgeFallbackTopics(ownerId, limit));

        List<String> deduped = dedupeTopics(fallback, limit);
        if (!deduped.isEmpty()) {
            return deduped;
        }
        throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未找到可用热点、爆款或知识库选题，请先补充数据源");
    }

    private List<String> getViralVideoTopics(Long ownerId, int limit) {
        List<SvViralVideo> videos = svViralVideoRepository.findByOwnerIdAndDeletedOrderByViewCountDesc(
                ownerId, 0, PageRequest.of(0, Math.max(1, limit))
        );
        return videos.stream()
                .map(SvViralVideo::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .toList();
    }

    private List<String> getKnowledgeFallbackTopics(Long userId, int limit) {
        if (knowledgeBaseService == null || userId == null) {
            return List.of();
        }
        try {
            List<RagRetrieveItemVO> results = knowledgeBaseService.hybridSearchAllKbs(
                    userId, "短视频 选题 热点 内容 方向", Math.max(limit, 3), "shortvideo", null);
            if (results == null || results.isEmpty()) {
                return List.of();
            }
            List<String> topics = new ArrayList<>();
            for (RagRetrieveItemVO result : results) {
                if (result.getTitle() != null && !result.getTitle().isBlank()) {
                    topics.add(result.getTitle().trim());
                } else if (result.getContent() != null && !result.getContent().isBlank()) {
                    String firstSentence = result.getContent().replaceAll("\\s+", " ").trim();
                    if (firstSentence.length() > 40) {
                        firstSentence = firstSentence.substring(0, 40);
                    }
                    topics.add(firstSentence);
                }
            }
            return topics;
        } catch (Exception e) {
            log.warn("知识库兜底选题检索失败: ownerId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    private List<String> dedupeTopics(List<String> topics, int limit) {
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String topic : topics) {
            if (topic == null) {
                continue;
            }
            String normalized = topic.replaceAll("\\s+", " ").trim();
            if (normalized.isEmpty() || out.contains(normalized)) {
                continue;
            }
            out.add(normalized);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    /**
     * 通过知识库 RAG 检索与话题相关的素材，返回标题+片段列表
     */
    private List<Map<String, String>> retrieveKnowledgeForTopic(String topic, Long userId) {
        if (knowledgeBaseService == null) {
            return List.of();
        }
        try {
            List<RagRetrieveItemVO> results = knowledgeBaseService.hybridSearchAllKbs(
                    userId, topic, 3, "shortvideo", null);
            if (results == null || results.isEmpty()) {
                return List.of();
            }
            return results.stream()
                    .map(r -> {
                        Map<String, String> ref = new LinkedHashMap<>();
                        ref.put("title", r.getTitle() != null ? r.getTitle() : "");
                        String snippet = r.getContent() != null && r.getContent().length() > 200
                                ? r.getContent().substring(0, 200) + "..."
                                : (r.getContent() != null ? r.getContent() : "");
                        ref.put("snippet", snippet);
                        ref.put("source", r.getSource() != null ? r.getSource() : "");
                        return ref;
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("RAG 检索失败: topic={}, error={}", topic, e.getMessage());
            return List.of();
        }
    }
}
