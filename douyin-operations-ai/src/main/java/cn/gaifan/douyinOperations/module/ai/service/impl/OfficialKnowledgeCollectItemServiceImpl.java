package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiOfficialKnowledgeCollectItem;
import cn.gaifan.douyinOperations.module.ai.repository.AiOfficialKnowledgeCollectItemRepository;
import cn.gaifan.douyinOperations.module.ai.service.OfficialKnowledgeCollectItemService;
import cn.gaifan.douyinOperations.module.ai.vo.OfficialKnowledgeCollectSearchVO;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OfficialKnowledgeCollectItemServiceImpl implements OfficialKnowledgeCollectItemService {

    private static final String STATUS_DISCOVERED = "DISCOVERED";
    private static final String STATUS_INDEXED = "INDEXED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_NOT_REQUIRED = "NOT_REQUIRED";

    @Resource
    private AiOfficialKnowledgeCollectItemRepository repository;

    @Override
    public PageResultVO<AiOfficialKnowledgeCollectItem> search(OfficialKnowledgeCollectSearchVO vo) {
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 200) : 30;
        Page<AiOfficialKnowledgeCollectItem> result = repository.findAll(spec(vo),
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "updateTime")));
        return PageResultVO.of(result.getTotalElements(), result.getContent(), page, rows);
    }

    @Override
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        long total = repository.countActive();
        long failed = repository.countFailedAny();
        long mediaPending = repository.countMediaStatus(STATUS_PENDING);
        result.put("total", total);
        result.put("failed", failed);
        result.put("mediaPending", mediaPending);
        result.put("complete", total > 0 && failed == 0 && mediaPending == 0);
        result.put("collectStatus", toCountMap(repository.countByCollectStatus()));
        result.put("indexStatus", toCountMap(repository.countByIndexStatus()));
        result.put("ocrStatus", toCountMap(repository.countByOcrStatus()));
        result.put("asrStatus", toCountMap(repository.countByAsrStatus()));
        result.put("targetKb", toCountMap(repository.countByTargetKbName()));
        result.put("checkedAt", Instant.now().toString());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDiscovered(CollectItemEvent event) {
        AiOfficialKnowledgeCollectItem item = upsert(event);
        item.setCollectStatus(STATUS_DISCOVERED);
        item.setIndexStatus(STATUS_PENDING);
        item.setLastDiscoveredAt(now());
        item.setLastError(null);
        applyCommon(item, event);
        repository.save(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markIndexed(CollectItemEvent event) {
        AiOfficialKnowledgeCollectItem item = upsert(event);
        applyCommon(item, event);
        item.setCollectStatus(STATUS_INDEXED);
        item.setIndexStatus(STATUS_INDEXED);
        item.setDocId(event.docId());
        item.setLastCollectedAt(now());
        item.setLastIndexedAt(now());
        item.setLastMediaExtractAt(mediaTouched(event) ? now() : item.getLastMediaExtractAt());
        item.setOcrStatus(resolveMediaStatus(event.imageCount(), event.imageTextCount()));
        item.setAsrStatus(resolveMediaStatus(event.videoCount(), event.videoTextCount()));
        item.setLastError(null);
        repository.save(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSkipped(CollectItemEvent event) {
        AiOfficialKnowledgeCollectItem item = upsert(event);
        applyCommon(item, event);
        item.setCollectStatus(STATUS_SKIPPED);
        item.setIndexStatus(STATUS_NOT_REQUIRED);
        item.setOcrStatus(resolveMediaStatus(event.imageCount(), event.imageTextCount()));
        item.setAsrStatus(resolveMediaStatus(event.videoCount(), event.videoTextCount()));
        item.setLastCollectedAt(now());
        item.setLastError(truncate(event.error(), 1000));
        repository.save(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(CollectItemEvent event) {
        AiOfficialKnowledgeCollectItem item = upsert(event);
        applyCommon(item, event);
        item.setCollectStatus(STATUS_FAILED);
        item.setIndexStatus(STATUS_FAILED);
        item.setRetryCount((item.getRetryCount() != null ? item.getRetryCount() : 0) + 1);
        item.setLastCollectedAt(now());
        item.setNextRetryAt(new Timestamp(System.currentTimeMillis() + 60L * 60L * 1000L));
        item.setLastError(truncate(event.error(), 2000));
        repository.save(item);
    }

    private Specification<AiOfficialKnowledgeCollectItem> spec(OfficialKnowledgeCollectSearchVO vo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(vo.getKeyword())) {
                String like = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("sourceUrl"), like),
                        cb.like(root.get("lastError"), like)
                ));
            }
            equalIfPresent(predicates, cb, root.get("sourceType"), vo.getSourceType());
            equalIfPresent(predicates, cb, root.get("category"), vo.getCategory());
            equalIfPresent(predicates, cb, root.get("topicCode"), vo.getTopicCode());
            equalIfPresent(predicates, cb, root.get("targetKbName"), vo.getTargetKbName());
            equalIfPresent(predicates, cb, root.get("collectStatus"), vo.getCollectStatus());
            equalIfPresent(predicates, cb, root.get("indexStatus"), vo.getIndexStatus());
            equalIfPresent(predicates, cb, root.get("ocrStatus"), vo.getOcrStatus());
            equalIfPresent(predicates, cb, root.get("asrStatus"), vo.getAsrStatus());
            if (vo.getViolation() != null) {
                predicates.add(cb.equal(root.get("violation"), vo.getViolation()));
            }
            if (Boolean.TRUE.equals(vo.getFailedOnly())) {
                predicates.add(cb.or(
                        cb.equal(root.get("collectStatus"), STATUS_FAILED),
                        cb.equal(root.get("indexStatus"), STATUS_FAILED),
                        cb.equal(root.get("ocrStatus"), STATUS_FAILED),
                        cb.equal(root.get("asrStatus"), STATUS_FAILED)
                ));
            }
            if (Boolean.TRUE.equals(vo.getMediaPendingOnly())) {
                predicates.add(cb.or(
                        cb.equal(root.get("ocrStatus"), STATUS_PENDING),
                        cb.equal(root.get("asrStatus"), STATUS_PENDING)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AiOfficialKnowledgeCollectItem upsert(CollectItemEvent event) {
        String url = StringUtils.hasText(event.sourceUrl()) ? event.sourceUrl().trim() : "unknown:" + event.title();
        String sourceType = StringUtils.hasText(event.sourceType()) ? event.sourceType().trim() : "douyin_school_official";
        String hash = hash(url);
        return repository.findBySourceTypeAndSourceUrlHashAndDeleted(sourceType, hash, 0)
                .orElseGet(() -> {
                    AiOfficialKnowledgeCollectItem created = new AiOfficialKnowledgeCollectItem();
                    created.setSourceType(sourceType);
                    created.setSourceUrl(url);
                    created.setSourceUrlHash(hash);
                    created.setLastDiscoveredAt(now());
                    return created;
                });
    }

    private void applyCommon(AiOfficialKnowledgeCollectItem item, CollectItemEvent event) {
        item.setSourceSite(StringUtils.hasText(event.sourceSite()) ? event.sourceSite() : "school.jinritemai.com");
        if (StringUtils.hasText(event.sourceUrl())) {
            item.setSourceUrl(event.sourceUrl().trim());
            item.setSourceUrlHash(hash(event.sourceUrl().trim()));
        }
        item.setSourceId(emptyToNull(event.sourceId()));
        item.setTitle(truncate(event.title(), 512));
        item.setCategory(emptyToNull(event.category()));
        item.setTopicCode(emptyToNull(event.topicCode()));
        item.setTargetKbName(emptyToNull(event.targetKbName()));
        item.setTargetKbId(event.targetKbId());
        item.setViolation(event.violation());
        item.setImageCount(Math.max(0, event.imageCount()));
        item.setVideoCount(Math.max(0, event.videoCount()));
        item.setImageTextCount(Math.max(0, event.imageTextCount()));
        item.setVideoTextCount(Math.max(0, event.videoTextCount()));
        item.setOfficialUpdateTimestamp(event.officialUpdateTimestamp() > 0 ? event.officialUpdateTimestamp() : null);
        item.setMetadata(StringUtils.hasText(event.metadata()) ? event.metadata() : JSON.toJSONString(Map.of(
                "sourceUrl", item.getSourceUrl(),
                "title", item.getTitle()
        )));
    }

    private static String resolveMediaStatus(int mediaCount, int textCount) {
        if (mediaCount <= 0) return STATUS_NOT_REQUIRED;
        if (textCount > 0) return STATUS_DONE;
        return STATUS_PENDING;
    }

    private static boolean mediaTouched(CollectItemEvent event) {
        return event.imageCount() > 0 || event.videoCount() > 0
                || event.imageTextCount() > 0 || event.videoTextCount() > 0;
    }

    private static Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    private static void equalIfPresent(List<Predicate> predicates,
                                       jakarta.persistence.criteria.CriteriaBuilder cb,
                                       jakarta.persistence.criteria.Path<String> path,
                                       String value) {
        if (StringUtils.hasText(value)) {
            predicates.add(cb.equal(path, value.trim()));
        }
    }

    private static String hash(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private static String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
