package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import cn.gaifan.douyinOperations.module.script.entity.ScriptLibrary;
import cn.gaifan.douyinOperations.module.script.repository.ScriptLibraryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播话术服务实现
 */
@Service
public class LiveScriptServiceImpl implements LiveScriptService {

    @Resource
    private LiveScriptRepository liveScriptRepository;
    @Resource
    private LiveProductRepository liveProductRepository;

    @Resource
    private ScriptLibraryRepository scriptLibraryRepository;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "sessionId", "sequenceNo", "executionTime", "executed", "createTime", "updateTime")));

    @Override
    public PageResultVO<LiveScriptVO> search(LiveScriptSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        if (vo.getSessionId() == null && vo.getSessionIds() != null && vo.getSessionIds().isEmpty()) {
            return PageResultVO.of(0L, Collections.emptyList(), vo.getPage(), vo.getRows());
        }

        Page<LiveScript> page;
        if (hasAdvancedFilter(vo)) {
            List<Long> sessionIds = vo.getSessionIds() == null || vo.getSessionIds().isEmpty()
                    ? List.of(-1L)
                    : vo.getSessionIds();
            page = liveScriptRepository.searchByFilters(
                    validPositive(vo.getSessionId()) ? vo.getSessionId() : null,
                    sessionIds,
                    vo.getSessionIds() != null && !vo.getSessionIds().isEmpty(),
                    trimToNull(vo.getScriptType()),
                    trimToNull(vo.getKeyword()),
                    vo.getExecuted(),
                    pageable);
        } else if (vo.getSessionId() != null && vo.getSessionId() > 0) {
            page = liveScriptRepository.findBySessionIdAndDeleted(vo.getSessionId(), 0, pageable);
        } else if (vo.getSessionIds() != null && !vo.getSessionIds().isEmpty()) {
            page = liveScriptRepository.findBySessionIdInAndDeleted(vo.getSessionIds(), 0, pageable);
        } else {
            page = liveScriptRepository.findAll(pageable);
        }

        List<LiveScriptVO> list = page.getContent().stream().map(this::toLiveScriptVO).collect(Collectors.toList());
        long total = page.getTotalElements();

        return PageResultVO.of(total, list, vo.getPage(), vo.getRows());
    }

    // P0-3: 添加缓存 - 话术详情查询
    @Override
    @Cacheable(value = "live:script", key = "#id")
    public LiveScriptVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播话术 ID 无效");
        }
        LiveScript script = liveScriptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
        return toLiveScriptVO(script);
    }

    // P0-3: 保存时清除缓存
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "live:script", key = "#vo.id", condition = "#vo.id != null")
    public long save(LiveScriptSaveVO vo) {
        LiveScript script;
        if (vo.getId() != null && vo.getId() > 0) {
            script = liveScriptRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
            if (vo.getSessionId() != null) {
                if (vo.getSessionId() <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
                }
                script.setSessionId(vo.getSessionId());
            }
        } else {
            if (vo.getSessionId() == null || vo.getSessionId() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
            }
            if (vo.getScriptContent() == null || vo.getScriptContent().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容不能为空");
            }
            script = new LiveScript();
            script.setSessionId(vo.getSessionId());
        }
        if (vo.getScriptContent() != null) {
            if (vo.getScriptContent().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术内容不能为空");
            }
            script.setScriptContent(vo.getScriptContent());
        }
        if (trimToNull(vo.getScriptType()) != null) {
            script.setScriptType(trimToNull(vo.getScriptType()));
        }
        if (vo.getStyle() != null) {
            script.setStyle(trimToNull(vo.getStyle()));
        }
        if (vo.getRequirement() != null) {
            script.setRequirement(trimToNull(vo.getRequirement()));
        }
        if (vo.getProductId() != null) {
            script.setProductId(vo.getProductId());
        }
        if (vo.getDurationLimitSec() != null) {
            script.setDurationLimitSec(vo.getDurationLimitSec());
        }
        if (vo.getSequenceNo() != null) {
            script.setSequenceNo(vo.getSequenceNo());
        }
        if (vo.getExecutionTime() != null) {
            script.setExecutionTime(vo.getExecutionTime());
        }
        if (vo.getExecuted() != null) {
            script.setExecuted(vo.getExecuted());
        }
        script = liveScriptRepository.save(script);
        return script.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // P0-3: 删除时清除缓存
    @CacheEvict(value = "live:script", key = "#id")
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播话术 ID 无效");
        }
        LiveScript script = liveScriptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
        script.setDeleted(1);
        liveScriptRepository.save(script);
    }

    @Override
    public List<LiveScriptVO> getBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        return liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0).stream()
                .map(this::toLiveScriptVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExecuted(Long id, Integer executed) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播话术 ID 无效");
        }
        LiveScript script = liveScriptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
        script.setExecuted(executed);
        if (executed == 1) {
            script.setActualExecutionTime(new Timestamp(System.currentTimeMillis()));
        }
        liveScriptRepository.save(script);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        liveScriptRepository.deleteBySessionId(sessionId);
    }

    @Override
    public List<LiveScriptVO> getEffectiveness(Long sessionId) {
        return getBySessionId(sessionId);
    }

    @Override
    public long saveToLibrary(Long scriptId, Long userId) {
        if (scriptId == null || scriptId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数无效");
        }
        LiveScript script = liveScriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
        if (script.getScriptContent() == null || script.getScriptContent().isBlank()
                || "[待填写]".equals(script.getScriptContent().trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "空话术不能保存到话术库");
        }
        ScriptLibrary library = new ScriptLibrary();
        library.setUserId(userId);
        library.setTitle(buildLibraryTitle(script));
        library.setContent(script.getScriptContent());
        library.setCategory(script.getScriptType());
        library.setSource("live");
        library.setSourceId(script.getSessionId());
        library.setStatus(1);
        return scriptLibraryRepository.save(library).getId();
    }

    @Override
    public int saveBatchToLibrary(Long sessionId, List<Long> scriptIds, Long userId) {
        if (scriptIds == null || scriptIds.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (Long scriptId : scriptIds) {
            try {
                saveToLibrary(scriptId, userId);
                saved++;
            } catch (BusinessException e) {
                if (e.getErrorCode() != ErrorCode.VALIDATION_FAIL) {
                    throw e;
                }
            }
        }
        return saved;
    }

    @Override
    public java.util.List<Long> findSessionIdsByUserIds(java.util.List<Long> userIds) {
        return liveSessionRepository.findIdsByUserIdIn(userIds);
    }

    @Override
    public String exportScripts(Long sessionId) {
        List<LiveScriptVO> list = getBySessionId(sessionId);
        StringBuilder sb = new StringBuilder();
        for (LiveScriptVO vo : list) {
            if (vo.getScriptContent() != null) {
                sb.append(vo.getScriptContent()).append("\n\n");
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureScriptSlotsForSession(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        List<LiveScript> existing = liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (!existing.isEmpty()) {
            return;
        }
        List<cn.gaifan.douyinOperations.module.live.entity.LiveProduct> products =
                liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(sessionId);

        // P1-12: 批量保存，避免 N+1 写入
        List<LiveScript> scripts = new ArrayList<>();
        int seq = 1;

        LiveScript opening = new LiveScript();
        opening.setSessionId(sessionId);
        opening.setScriptType("opening");
        opening.setScriptContent("[待填写]");
        opening.setSequenceNo(seq++);
        scripts.add(opening);

        for (int i = 0; i < products.size(); i++) {
            LiveScript productSlot = new LiveScript();
            productSlot.setSessionId(sessionId);
            productSlot.setScriptType("product");
            productSlot.setProductId(products.get(i).getProductId());
            productSlot.setScriptContent("[待填写]");
            productSlot.setSequenceNo(seq++);
            scripts.add(productSlot);

            if (i < products.size() - 1) {
                LiveScript transitionSlot = new LiveScript();
                transitionSlot.setSessionId(sessionId);
                transitionSlot.setScriptType("transition");
                transitionSlot.setScriptContent("[待填写]");
                transitionSlot.setSequenceNo(seq++);
                scripts.add(transitionSlot);
            }
        }

        LiveScript closing = new LiveScript();
        closing.setSessionId(sessionId);
        closing.setScriptType("closing");
        closing.setScriptContent("[待填写]");
        closing.setSequenceNo(seq);
        scripts.add(closing);

        liveScriptRepository.saveAll(scripts);
    }

    private LiveScriptVO toLiveScriptVO(LiveScript script) {
        LiveScriptVO vo = new LiveScriptVO();
        vo.setId(script.getId());
        vo.setSessionId(script.getSessionId());
        vo.setScriptContent(script.getScriptContent());
        vo.setSequenceNo(script.getSequenceNo());
        vo.setExecutionTime(script.getExecutionTime());
        vo.setExecuted(script.getExecuted());
        vo.setActualExecutionTime(script.getActualExecutionTime());

        // P0-2: 补全所有 Entity 字段
        vo.setScriptType(script.getScriptType());
        vo.setStyle(script.getStyle());
        vo.setAiGenerated(script.getAiGenerated() != null && script.getAiGenerated() == 1);
        vo.setProductId(script.getProductId());
        vo.setAiCallLogId(script.getAiCallLogId());
        vo.setGenerationStatus(script.getGenerationStatus());
        vo.setViolationChecked(script.getViolationChecked() != null && script.getViolationChecked() == 1);
        vo.setViolationResult(script.getViolationResult());
        vo.setViewerDelta(script.getViewerDelta());
        vo.setInteractionDelta(script.getInteractionDelta());
        vo.setConversionDelta(script.getConversionDelta());
        vo.setEffectivenessScore(script.getEffectivenessScore() != null ? script.getEffectivenessScore().doubleValue() : null);
        vo.setDurationLimitSec(script.getDurationLimitSec());
        vo.setRequirement(script.getRequirement());
        vo.setReferencedScriptId(script.getReferencedScriptId());
        vo.setReferencedScriptSnapshot(script.getReferencedScriptSnapshot());
        vo.setApprovalStatus(script.getApprovalStatus() != null ? script.getApprovalStatus().toString() : null);
        vo.setUserId(script.getUserId());
        vo.setGenerationPromptHash(script.getGenerationPromptHash());
        vo.setAbExperimentId(script.getAbExperimentId());
        vo.setAbVariantId(script.getAbVariantId());
        vo.setAiSuggestion(script.getAiSuggestion());
        vo.setPromptTemplateId(script.getPromptTemplateId());

        vo.setCreateTime(script.getCreateTime());
        vo.setUpdateTime(script.getUpdateTime());
        return vo;
    }

    private String buildLibraryTitle(LiveScript script) {
        String type = script.getScriptType() != null ? script.getScriptType() : "live";
        return "直播话术-" + type + "-" + script.getId();
    }

    private static boolean hasAdvancedFilter(LiveScriptSearchVO vo) {
        return vo.getExecuted() != null
                || trimToNull(vo.getScriptType()) != null
                || trimToNull(vo.getKeyword()) != null;
    }

    private static boolean validPositive(Long value) {
        return value != null && value > 0;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
