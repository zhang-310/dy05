package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.*;
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

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "sessionId", "sequenceNo", "executionTime", "executed", "createTime", "updateTime")));

    @Override
    public PageResultVO<LiveScriptVO> search(LiveScriptSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Page<LiveScript> page;
        if (vo.getSessionId() != null && vo.getSessionId() > 0) {
            if (vo.getExecuted() != null) {
                List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndExecutedAndDeleted(
                        vo.getSessionId(), vo.getExecuted(), 0);
                page = null;
            } else {
                page = liveScriptRepository.findBySessionIdAndDeleted(vo.getSessionId(), 0, pageable);
            }
        } else if (vo.getSessionIds() != null && !vo.getSessionIds().isEmpty()) {
            page = liveScriptRepository.findBySessionIdInAndDeleted(vo.getSessionIds(), 0, pageable);
        } else {
            page = liveScriptRepository.findAll(pageable);
        }

        List<LiveScriptVO> list;
        long total;
        if (page != null) {
            list = page.getContent().stream().map(this::toLiveScriptVO).collect(Collectors.toList());
            total = page.getTotalElements();
        } else {
            list = liveScriptRepository.findBySessionIdAndDeleted(vo.getSessionId(), 0).stream()
                    .map(this::toLiveScriptVO).collect(Collectors.toList());
            total = list.size();
        }

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
        if (vo.getSessionId() == null || vo.getSessionId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }

        LiveScript script;
        if (vo.getId() != null && vo.getId() > 0) {
            script = liveScriptRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播话术不存在"));
        } else {
            script = new LiveScript();
            script.setSessionId(vo.getSessionId());
        }
        script.setScriptContent(vo.getScriptContent());
        script.setSequenceNo(vo.getSequenceNo());
        script.setExecutionTime(vo.getExecutionTime());
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
        return liveScriptRepository.findBySessionIdAndDeleted(sessionId, 0).stream()
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
        // TODO: 接入话术库后实现
        return 0L;
    }

    @Override
    public int saveBatchToLibrary(Long sessionId, List<Long> scriptIds, Long userId) {
        // TODO: 接入话术库后实现
        return 0;
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
        List<LiveScript> existing = liveScriptRepository.findBySessionIdAndDeleted(sessionId, 0);
        if (!existing.isEmpty()) {
            return;
        }
        List<cn.gaifan.douyinOperations.module.live.entity.LiveProduct> products =
                liveProductRepository.findBySessionId(sessionId);

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
}
