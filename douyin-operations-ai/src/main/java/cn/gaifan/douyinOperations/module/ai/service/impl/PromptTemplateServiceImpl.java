package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.repository.AiPromptTemplateRepository;
import cn.gaifan.douyinOperations.module.ai.service.PromptTemplateService;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSearchVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateServiceImpl.class);
    private static final String CACHE_PREFIX = "prompt_tpl:active:";
    private static final long CACHE_TTL_MINUTES = 5;

    @Resource
    private AiPromptTemplateRepository repository;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PageResultVO<AiPromptTemplate> search(AiPromptTemplateSearchVO searchVO) {
        if (searchVO == null) searchVO = new AiPromptTemplateSearchVO();
        searchVO.validateParams();

        int page = searchVO.getPage() != null ? searchVO.getPage() : 0;
        int rows = searchVO.getRows() != null ? searchVO.getRows() : 30;
        String sortName = searchVO.getSortName() != null ? searchVO.getSortName() : "id";
        String sortOrder = searchVO.getSortOrder() != null ? searchVO.getSortOrder() : "desc";

        String templateCode = searchVO.getTemplateCode();
        String variantName = searchVO.getVariantName();
        Boolean isActive = searchVO.getIsActive();
        Long ownerId = searchVO.getOwnerId();

        Specification<AiPromptTemplate> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("deleted"), 0));

            if (templateCode != null && !templateCode.trim().isEmpty()) {
                preds.add(cb.equal(root.get("templateCode"), templateCode.trim()));
            }
            if (variantName != null && !variantName.trim().isEmpty()) {
                preds.add(cb.equal(root.get("variantName"), variantName.trim()));
            }
            if (isActive != null) {
                preds.add(cb.equal(root.get("isActive"), isActive));
            }
            if (ownerId != null) {
                preds.add(cb.equal(root.get("ownerId"), ownerId));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };

        Sort sort = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.by(Sort.Direction.DESC, sortName)
                : Sort.by(Sort.Direction.ASC, sortName);
        Pageable pageable = PageRequest.of(page, rows, sort);

        var pg = repository.findAll(spec, pageable);
        return PageResultVO.of(pg.getTotalElements(), pg.getContent(), page, rows);
    }

    @Override
    public AiPromptTemplate getById(Long id) {
        return repository.findById(id)
                .filter(e -> e.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "提示词模板不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiPromptTemplate save(AiPromptTemplateSaveVO vo) {
        AiPromptTemplate entity;
        if (vo.getId() != null) {
            entity = getById(vo.getId());
        } else {
            entity = new AiPromptTemplate();
        }

        if (vo.getTemplateName() != null) entity.setTemplateName(vo.getTemplateName());
        if (vo.getTemplateContent() != null) entity.setTemplateContent(vo.getTemplateContent());
        if (vo.getCategory() != null) entity.setCategory(vo.getCategory());
        if (vo.getVariables() != null) entity.setVariables(vo.getVariables());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getTemplateCode() != null) entity.setTemplateCode(vo.getTemplateCode());
        if (vo.getVariantName() != null) entity.setVariantName(vo.getVariantName());
        if (vo.getVersion() != null) entity.setVersion(String.valueOf(vo.getVersion()));
        if (vo.getSystemPrompt() != null) entity.setSystemPrompt(vo.getSystemPrompt());
        if (vo.getUserPromptTpl() != null) entity.setUserPromptTpl(vo.getUserPromptTpl());
        if (vo.getModelHint() != null) entity.setModelHint(vo.getModelHint());
        if (vo.getTemperature() != null) entity.setTemperature(vo.getTemperature());
        if (vo.getMaxTokens() != null) entity.setMaxTokens(vo.getMaxTokens());
        if (vo.getIsActive() != null) entity.setIsActive(Boolean.TRUE.equals(vo.getIsActive()) ? 1 : 0);
        if (vo.getIsDefault() != null) entity.setIsDefault(Boolean.TRUE.equals(vo.getIsDefault()) ? 1 : 0);
        if (vo.getUsageCount() != null) entity.setUsageCount(vo.getUsageCount());
        if (vo.getAvgScore() != null) entity.setAvgScore(vo.getAvgScore());
        if (vo.getP50Score() != null) entity.setP50Score(vo.getP50Score());
        if (vo.getP90Score() != null) entity.setP90Score(vo.getP90Score());
        if (vo.getOwnerId() != null) entity.setOwnerId(vo.getOwnerId());
        if (vo.getUserId() != null) entity.setUserId(vo.getUserId());

        // 新增时确保必填字段
        if (entity.getId() == null) {
            if (entity.getTemplateContent() == null) entity.setTemplateContent("");
            if (entity.getUserId() == null) entity.setUserId(0L);
        }

        AiPromptTemplate saved = repository.save(entity);
        evictCache(saved.getTemplateCode(), saved.getVariantName(), saved.getOwnerId());
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        repository.findById(id).ifPresent(e -> {
            e.setDeleted(1);
            repository.save(e);
            evictCache(e.getTemplateCode(), e.getVariantName(), e.getOwnerId());
        });
    }

    @Override
    public AiPromptTemplate getActiveTemplate(String templateCode, String variantName, Long ownerId) {
        if (templateCode == null || templateCode.isBlank()) return null;
        if (variantName == null || variantName.isBlank()) variantName = "default";

        // 尝试从 Redis 缓存读取
        String cacheKey = CACHE_PREFIX + templateCode + ":" + variantName + ":" + ownerId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if ("NULL".equals(cached)) return null;
                return objectMapper.readValue(cached, AiPromptTemplate.class);
            }
        } catch (Exception e) {
            log.warn("读取提示词模板缓存失败: {}", e.getMessage());
        }

        // 回退链：用户模板 → 系统模板 → null（单次查询）
        List<AiPromptTemplate> candidates = repository
                .findByTemplateCodeAndVariantNameAndIsActiveAndDeleted(templateCode, variantName, 1, 0);

        AiPromptTemplate result = null;

        // 第一优先：用户专属模板
        if (ownerId != null && ownerId > 0) {
            result = candidates.stream()
                    .filter(t -> ownerId.equals(t.getOwnerId()))
                    .findFirst()
                    .orElse(null);
        }

        // 第二优先：系统级模板（ownerId = 0）
        if (result == null) {
            result = candidates.stream()
                    .filter(t -> t.getOwnerId() != null && t.getOwnerId() == 0L)
                    .findFirst()
                    .orElse(null);
        }

        // 写入缓存
        try {
            String val = result != null ? objectMapper.writeValueAsString(result) : "NULL";
            redisTemplate.opsForValue().set(cacheKey, val, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入提示词模板缓存失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsage(Long templateId) {
        repository.findById(templateId).ifPresent(t -> {
            t.setUsageCount(t.getUsageCount() != null ? t.getUsageCount() + 1 : 1);
            repository.save(t);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long templateId) {
        repository.findById(templateId)
                .filter(e -> e.getDeleted() == 0)
                .ifPresent(t -> {
                    t.setUsageCount(t.getUsageCount() != null ? t.getUsageCount() + 1 : 1);
                    t.setLastUsedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                    repository.save(t);
                    log.info("模板使用统计更新: templateId={}, usageCount={}", templateId, t.getUsageCount());
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEffectivenessScore(Long templateId, float score) {
        repository.findById(templateId)
                .filter(e -> e.getDeleted() == 0)
                .ifPresent(t -> {
                    int count = t.getUsageCount() != null ? t.getUsageCount() : 0;
                    float oldAvg = t.getAvgScore() != null ? t.getAvgScore() : 0f;
                    // 滑动平均：newAvg = (oldAvg * count + score) / (count + 1)
                    float newAvg = count > 0 ? (oldAvg * count + score) / (count + 1) : score;
                    t.setAvgScore(newAvg);
                    repository.save(t);
                });
    }

    /**
     * 清除缓存
     */
    private void evictCache(String templateCode, String variantName, Long ownerId) {
        if (templateCode == null) return;
        try {
            String variant = variantName != null ? variantName : "default";
            Long owner = ownerId != null ? ownerId : 0L;
            String cacheKey = CACHE_PREFIX + templateCode + ":" + variant + ":" + owner;
            redisTemplate.delete(cacheKey);
            // 也清除系统级缓存
            String sysCacheKey = CACHE_PREFIX + templateCode + ":" + variant + ":0";
            redisTemplate.delete(sysCacheKey);
        } catch (Exception e) {
            log.warn("清除提示词模板缓存失败: {}", e.getMessage());
        }
    }
}
