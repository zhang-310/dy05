package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.TaskModelConfigService;
import cn.gaifan.douyinOperations.module.ai.vo.TaskModelConfigRowVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskModelConfigServiceImpl implements TaskModelConfigService {

    @Resource
    private AiTaskModelConfigRepository repository;

    @Resource
    private AiModelRepository aiModelRepository;

    @Override
    public List<TaskModelConfigRowVO> listAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    public TaskModelConfigRowVO getRowById(Long id) {
        return toRow(getById(id));
    }

    @Override
    public AiTaskModelConfig getById(Long id) {
        return repository.findById(id)
                .filter(e -> e.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务配置不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(AiTaskModelConfig config) {
        if (config.getTaskCode() == null || config.getTaskCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskCode 不能为空");
        }
        if (config.getTaskName() == null || config.getTaskName().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskName 不能为空");
        }
        String taskCode = config.getTaskCode().trim();
        config.setTaskCode(taskCode);
        repository.findByTaskCodeAndStatusAndDeleted(taskCode, 1, 0).ifPresent(existing -> {
            if (config.getId() == null || !existing.getId().equals(config.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskCode 已存在或与其他配置冲突");
            }
        });
        if (config.getTaskGroup() == null || config.getTaskGroup().isBlank()) {
            config.setTaskGroup("evolve");
        }
        if (config.getMaxRetries() == null) config.setMaxRetries(1);
        if (config.getSortOrder() == null) config.setSortOrder(0);
        if (config.getStatus() == null) config.setStatus(1);
        assertActiveModel(config.getPrimaryModelId(), "主模型");
        assertActiveModel(config.getFallbackModelId(), "备用模型 1");
        assertActiveModel(config.getFallback2ModelId(), "备用模型 2");
        return repository.save(config).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        repository.findById(id).ifPresent(e -> {
            e.setDeleted(1);
            repository.save(e);
        });
    }

    private TaskModelConfigRowVO toRow(AiTaskModelConfig e) {
        TaskModelConfigRowVO vo = new TaskModelConfigRowVO();
        vo.setId(e.getId());
        vo.setTaskCode(e.getTaskCode());
        vo.setTaskName(e.getTaskName());
        vo.setTaskGroup(e.getTaskGroup());
        vo.setPrimaryModelId(e.getPrimaryModelId());
        vo.setFallbackModelId(e.getFallbackModelId());
        vo.setFallback2ModelId(e.getFallback2ModelId());
        vo.setTimeoutSeconds(e.getTimeoutSeconds());
        vo.setMaxRetries(e.getMaxRetries());
        vo.setSortOrder(e.getSortOrder());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        vo.setPrimaryModelName(resolveModelName(e.getPrimaryModelId()));
        vo.setFallbackModelName(resolveModelName(e.getFallbackModelId()));
        vo.setFallback2ModelName(resolveModelName(e.getFallback2ModelId()));
        return vo;
    }

    private String resolveModelName(Long id) {
        if (id == null) {
            return null;
        }
        return aiModelRepository.findByIdAndDeleted(id, 0)
                .map(AiModel::getModelName)
                .orElse(null);
    }

    private void assertActiveModel(Long id, String label) {
        if (id == null) {
            return;
        }
        AiModel m = aiModelRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, label + " 对应的模型不存在或已删除"));
        if (m.getStatus() == null || m.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, label + " 对应的模型未启用");
        }
    }
}
