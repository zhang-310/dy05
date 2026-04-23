package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationPreset;
import cn.gaifan.douyinOperations.module.live.repository.LiveGenerationPresetRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationPresetService;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationPresetSaveVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 生成配置预设服务实现
 */
@Service
public class LiveGenerationPresetServiceImpl implements LiveGenerationPresetService {

    private static final Logger log = LoggerFactory.getLogger(LiveGenerationPresetServiceImpl.class);

    @Resource
    private LiveGenerationPresetRepository presetRepository;

    @Override
    public List<LiveGenerationPreset> list(Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return presetRepository.findByOwnerIdOrderByCreateTimeDesc(ownerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveGenerationPreset save(LiveGenerationPresetSaveVO vo, Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        LiveGenerationPreset preset;
        if (vo.getId() != null && vo.getId() > 0) {
            // 更新
            if (RequestRoleResolver.isAdmin()) {
                preset = presetRepository.findById(vo.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在"));
            } else {
                preset = presetRepository.findByIdAndOwnerId(vo.getId(), ownerId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在或无权访问"));
            }
        } else {
            // 新建
            preset = new LiveGenerationPreset();
            preset.setOwnerId(ownerId);
        }

        preset.setName(vo.getName());
        if (vo.getDescription() != null) {
            preset.setDescription(vo.getDescription());
        }
        if (vo.getStyle() != null) {
            preset.setStyle(vo.getStyle());
        }
        if (vo.getModelId() != null) {
            preset.setModelId(vo.getModelId());
        }
        if (vo.getUseKbRef() != null) {
            preset.setUseKbRef(vo.getUseKbRef());
        }
        if (vo.getDurationMode() != null) {
            preset.setDurationMode(vo.getDurationMode());
        }
        if (vo.getHotKeywords() != null) {
            preset.setHotKeywords(vo.getHotKeywords());
        }
        if (vo.getIpType() != null) {
            preset.setIpType(vo.getIpType());
        }
        if (vo.getMaterialType() != null) {
            preset.setMaterialType(vo.getMaterialType());
        }
        if (vo.getScriptModule() != null) {
            preset.setScriptModule(vo.getScriptModule());
        }
        if (vo.getRetentionStrategy() != null) {
            preset.setRetentionStrategy(vo.getRetentionStrategy());
        }
        if (vo.getInteractionLevel() != null) {
            preset.setInteractionLevel(vo.getInteractionLevel());
        }
        if (vo.getIsDefault() != null) {
            // 如果设为默认，先清除该用户其他默认标记
            if (Boolean.TRUE.equals(vo.getIsDefault())) {
                presetRepository.clearDefaultsByOwnerId(ownerId);
            }
            preset.setIsDefault(vo.getIsDefault());
        }

        preset = presetRepository.save(preset);
        log.info("保存生成预设: ownerId={}, presetId={}, name={}", ownerId, preset.getId(), preset.getName());
        return preset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ownerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "预设 ID 无效");
        }
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        LiveGenerationPreset preset;
        if (RequestRoleResolver.isAdmin()) {
            preset = presetRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在"));
        } else {
            preset = presetRepository.findByIdAndOwnerId(id, ownerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在或无权访问"));
        }
        preset.setDeleted(1);
        presetRepository.save(preset);
        log.info("删除生成预设: ownerId={}, presetId={}", ownerId, id);
    }

    @Override
    public LiveGenerationPreset getDefault(Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return presetRepository.findByOwnerIdAndIsDefaultTrue(ownerId).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long ownerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "预设 ID 无效");
        }
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        LiveGenerationPreset preset;
        if (RequestRoleResolver.isAdmin()) {
            preset = presetRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在"));
        } else {
            preset = presetRepository.findByIdAndOwnerId(id, ownerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "预设不存在或无权访问"));
        }
        presetRepository.clearDefaultsByOwnerId(ownerId);
        preset.setIsDefault(true);
        presetRepository.save(preset);
        log.info("设置默认预设: ownerId={}, presetId={}", ownerId, id);
    }
}
