package cn.gaifan.douyinOperations.module.slangdict.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.slangdict.entity.SdEntry;
import cn.gaifan.douyinOperations.module.slangdict.entity.SdProductMapping;
import cn.gaifan.douyinOperations.module.slangdict.repository.SdEntryRepository;
import cn.gaifan.douyinOperations.module.slangdict.repository.SdProductMappingRepository;
import cn.gaifan.douyinOperations.module.slangdict.service.SlangDictService;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySearchVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySaveVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntryVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlangDictServiceImpl implements SlangDictService {

    private static final Logger log = LoggerFactory.getLogger(SlangDictServiceImpl.class);

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "userId", "useCount", "status", "createTime", "updateTime");

    private static final String TASK_CODE_COPY_PROCESSING = "copy_processing";

    @Resource private SdEntryRepository entryRepository;
    @Resource private SdProductMappingRepository mappingRepository;
    @Resource private DyProductRepository productRepository;
    @Resource private LlmClient llmClient;
    @Resource private AiModelRepository aiModelRepository;
    @Resource private AiTaskModelConfigRepository taskModelConfigRepository;

    @Override
    public PageResultVO<SdEntryVO> search(SdEntrySearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        // 如果按产品 ID 搜索，先查关联的 entry IDs
        Set<Long> entryIdsForProduct = null;
        if (vo.getProductId() != null && vo.getProductId() > 0) {
            List<SdProductMapping> mappings = mappingRepository.findByProductIdAndUserIdAndDeleted(
                    vo.getProductId(), vo.getUserId(), 0);
            entryIdsForProduct = mappings.stream().map(SdProductMapping::getEntryId).collect(Collectors.toSet());
            if (entryIdsForProduct.isEmpty()) {
                return PageResultVO.of(0L, Collections.emptyList(), vo.getPage(), vo.getRows());
            }
        }

        final Set<Long> finalEntryIds = entryIdsForProduct;
        Specification<SdEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("phrase"), kw),
                        cb.like(root.get("meaning"), kw)
                ));
            }
            if (vo.getCategory() != null && !vo.getCategory().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), vo.getCategory().trim()));
            }
            if (vo.getUsageScene() != null && !vo.getUsageScene().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("usageScene"), vo.getUsageScene().trim()));
            }
            if (vo.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            if (finalEntryIds != null) {
                predicates.add(root.get("id").in(finalEntryIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SdEntry> page = entryRepository.findAll(spec, pageable);
        List<SdEntryVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SdEntryVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "梗条目 ID 无效");
        SdEntry entity = entryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "梗条目不存在"));
        SdEntryVO vo = toVO(entity);
        // 加载关联产品 ID
        List<SdProductMapping> mappings = mappingRepository.findByEntryIdAndDeleted(id, 0);
        vo.setProductIds(mappings.stream().map(SdProductMapping::getProductId).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SdEntrySaveVO vo, Long userId) {
        SdEntry entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = entryRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "梗条目不存在"));
        } else {
            entity = new SdEntry();
            entity.setUserId(userId);
        }
        entity.setPhrase(vo.getPhrase());
        entity.setMeaning(vo.getMeaning());
        entity.setCategory(vo.getCategory() != null ? vo.getCategory() : "general");
        entity.setUsageScene(vo.getUsageScene());
        entity.setExample(vo.getExample());
        entity.setSource(vo.getSource());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        entryRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "梗条目 ID 无效");
        SdEntry entity = entryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "梗条目不存在"));
        entity.setDeleted(1);
        entryRepository.save(entity);
    }

    @Override
    public List<SdEntryVO> getByProductId(Long productId, Long userId) {
        List<SdProductMapping> mappings = mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0);
        if (mappings.isEmpty()) return Collections.emptyList();
        Set<Long> entryIds = mappings.stream().map(SdProductMapping::getEntryId).collect(Collectors.toSet());
        return entryRepository.findAllById(entryIds).stream()
                .filter(e -> e.getDeleted() == 0 && e.getStatus() == 1)
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindProduct(Long entryId, Long productId, Long userId) {
        entryRepository.findByIdAndDeleted(entryId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "梗条目不存在"));
        // 检查是否已绑定
        List<SdProductMapping> existing = mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0);
        boolean alreadyBound = existing.stream().anyMatch(m -> m.getEntryId().equals(entryId));
        if (alreadyBound) return;

        SdProductMapping mapping = new SdProductMapping();
        mapping.setEntryId(entryId);
        mapping.setProductId(productId);
        mapping.setUserId(userId);
        mappingRepository.save(mapping);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindProduct(Long entryId, Long productId, Long userId) {
        mappingRepository.softDeleteByEntryAndProduct(entryId, productId, userId);
    }

    @Override
    public List<String> aiGeneratePhrases(Long productId, Long userId, int count) {
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "商品不存在"));
        if (count <= 0 || count > 20) count = 5;

        String system = "你是直播带货创意表达专家，擅长为产品创造有趣的暗语、梗和创意表达。";
        String prompt = String.format("""
                为以下产品生成 %d 条创意暗语/梗：
                产品名称：%s
                产品描述：%s
                价格：%s元

                要求：
                1. 每行一条，格式："暗语" → 真实含义
                2. 有趣、有记忆点、适合直播场景
                3. 口语化，朗朗上口
                4. 不要违禁词、不要低俗

                直接输出，不要序号和额外说明。
                """, count,
                product.getProductName(),
                product.getDescription() != null ? product.getDescription() : "暂无",
                product.getPrice() != null ? product.getPrice().toString() : "待定");

        List<AiModel> models = resolveModels();
        if (models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_MODEL_UNAVAILABLE, "无可用 AI 模型");
        }

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, system, prompt);
        if (!resp.success() || resp.content() == null) {
            throw new BusinessException(ErrorCode.AI_GENERATE_FAIL, "AI 生成失败: " + resp.errorMsg());
        }

        return Arrays.stream(resp.content().split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public String buildSlangContextForPrompt(Long productId, Long userId) {
        if (productId == null || userId == null) return "";
        List<SdEntryVO> entries = getByProductId(productId, userId);
        if (entries.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("【产品创意表达（请自然融入话术，增强记忆点和趣味性）】\n");
        for (SdEntryVO e : entries) {
            sb.append("- \"").append(e.getPhrase()).append("\"");
            if (e.getMeaning() != null && !e.getMeaning().isBlank()) {
                sb.append(" → ").append(e.getMeaning());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<AiModel> resolveModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(TASK_CODE_COPY_PROCESSING, 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long id : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (id == null) continue;
                aiModelRepository.findById(id).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0).ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        List<AiModel> all = aiModelRepository.findByStatusAndDeleted(1, 0);
        return all == null || all.isEmpty() ? Collections.emptyList() : List.of(all.get(0));
    }

    private SdEntryVO toVO(SdEntry entity) {
        SdEntryVO vo = new SdEntryVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setPhrase(entity.getPhrase());
        vo.setMeaning(entity.getMeaning());
        vo.setCategory(entity.getCategory());
        vo.setUsageScene(entity.getUsageScene());
        vo.setExample(entity.getExample());
        vo.setSource(entity.getSource());
        vo.setUseCount(entity.getUseCount());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
