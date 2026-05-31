package cn.gaifan.douyinOperations.module.digitalhuman.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.DigitalHumanProvider;
import cn.gaifan.douyinOperations.module.digitalhuman.entity.DigitalHumanTask;
import cn.gaifan.douyinOperations.module.digitalhuman.repository.DigitalHumanTaskRepository;
import cn.gaifan.douyinOperations.module.digitalhuman.service.DigitalHumanService;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSaveVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSearchVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanTaskVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;

@Service
public class DigitalHumanServiceImpl implements DigitalHumanService {

    private static final Logger log = LoggerFactory.getLogger(DigitalHumanServiceImpl.class);
    private static final Set<String> SORTABLE = Set.of("id", "status", "createTime");

    @Resource
    private DigitalHumanTaskRepository repo;

    @Autowired(required = false)
    private DigitalHumanProvider digitalHumanProvider;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Override
    public Map<String, Object> overview(Long userId) {
        List<DigitalHumanTask> tasks = repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DigitalHumanTask t : tasks) {
            byStatus.merge(t.getStatus(), 1L, Long::sum);
        }
        return Map.of(
                "productCode", "digital-human",
                "taskCount", tasks.size(),
                "byStatus", byStatus
        );
    }

    @Override
    public PageResultVO<DigitalHumanTaskVO> search(DigitalHumanSearchVO vo, Long userId) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<DigitalHumanTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus().trim()));
            }
            if (vo.getVoiceType() != null && !vo.getVoiceType().isBlank()) {
                predicates.add(cb.equal(root.get("voiceType"), vo.getVoiceType().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DigitalHumanTask> page = repo.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).toList(),
                vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(DigitalHumanSaveVO saveVO, Long userId) {
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.DIGITAL_HUMAN,
                            FeatureCode.DIGITAL_HUMAN_GENERATE,
                            "数字人任务创建 userId=" + userId,
                            DeliveryProduct.DIGITAL_HUMAN
                    ));
        }
        DigitalHumanTask task = new DigitalHumanTask();
        task.setUserId(userId);
        task.setScriptContent(saveVO.getScriptContent());
        task.setVoiceType(saveVO.getVoiceType() != null ? saveVO.getVoiceType() : "default");
        task.setStatus("pending");
        task.setCostCredits(50L);
        task.setProgress(0);
        task.setCreateTime(new Timestamp(System.currentTimeMillis()));
        task = repo.save(task);

        if (digitalHumanProvider != null && digitalHumanProvider.isConfigured()) {
            try {
                task.setStatus("processing");
                task.setProgress(30);
                repo.save(task);

                String avatarId = saveVO.getAvatarId() != null ? saveVO.getAvatarId() : "default";
                String outputUrl = digitalHumanProvider.generateTalkingHead(
                        avatarId, task.getScriptContent(), task.getVoiceType());
                task.setOutputUrl(outputUrl);
                task.setStatus("completed");
                task.setProgress(100);
            } catch (Exception e) {
                log.error("Digital human generation failed for task {}", task.getId(), e);
                task.setStatus("failed");
                task.setErrorMessage(e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                        : "Unknown error");
                task.setProgress(0);
            }
        }
        return repo.save(task).getId();
    }

    @Override
    public DigitalHumanTaskVO getStatus(Long taskId, Long userId) {
        DigitalHumanTask task = repo.findById(taskId)
                .filter(t -> t.getDeleted() == 0 && t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "数字人任务不存在"));
        return toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long taskId, Long userId) {
        DigitalHumanTask task = repo.findById(taskId)
                .filter(t -> t.getDeleted() == 0 && t.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "数字人任务不存在"));
        task.setDeleted(1);
        repo.save(task);
    }

    private DigitalHumanTaskVO toVO(DigitalHumanTask entity) {
        DigitalHumanTaskVO vo = new DigitalHumanTaskVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setScriptContent(entity.getScriptContent());
        vo.setVoiceType(entity.getVoiceType());
        vo.setAvatarId(entity.getAvatarId());
        vo.setStatus(entity.getStatus());
        vo.setOutputUrl(entity.getOutputUrl());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setProgress(entity.getProgress());
        vo.setCostCredits(entity.getCostCredits());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
