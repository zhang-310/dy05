package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import cn.gaifan.douyinOperations.module.live.repository.LiveEffectivenessConfigRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveEffectivenessConfigService;
import cn.gaifan.douyinOperations.module.live.vo.LiveEffectivenessConfigSaveVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 效果评分权重配置 Service 实现
 * Q3-5: Configurable Effectiveness Score Formula
 */
@Service
public class LiveEffectivenessConfigServiceImpl implements LiveEffectivenessConfigService {

    private static final Logger log = LoggerFactory.getLogger(LiveEffectivenessConfigServiceImpl.class);

    /** 系统默认权重 */
    private static final BigDecimal DEFAULT_CONVERSION_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal DEFAULT_INTERACTION_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_RETENTION_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal DEFAULT_GMV_WEIGHT = new BigDecimal("0.20");

    @Resource
    private LiveEffectivenessConfigRepository configRepository;

    @Override
    public LiveEffectivenessConfig getDefaultConfig(Long userId) {
        Optional<LiveEffectivenessConfig> userDefault = configRepository
                .findByUserIdAndIsDefaultAndDeleted(userId, 1, 0);

        if (userDefault.isPresent()) {
            return userDefault.get();
        }

        // 返回系统默认配置（不持久化，仅内存构建）
        LiveEffectivenessConfig systemDefault = new LiveEffectivenessConfig();
        systemDefault.setUserId(userId);
        systemDefault.setConfigName("系统默认配置");
        systemDefault.setConversionWeight(DEFAULT_CONVERSION_WEIGHT);
        systemDefault.setInteractionWeight(DEFAULT_INTERACTION_WEIGHT);
        systemDefault.setRetentionWeight(DEFAULT_RETENTION_WEIGHT);
        systemDefault.setGmvWeight(DEFAULT_GMV_WEIGHT);
        systemDefault.setIsDefault(1);
        return systemDefault;
    }

    @Override
    @Transactional
    public LiveEffectivenessConfig save(LiveEffectivenessConfigSaveVO vo, Long userId) {
        // 校验四项权重之和为 1.0
        BigDecimal totalWeight = vo.getConversionWeight()
                .add(vo.getInteractionWeight())
                .add(vo.getRetentionWeight())
                .add(vo.getGmvWeight());
        if (totalWeight.compareTo(BigDecimal.ONE) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "四项权重之和必须为 1.0，当前合计: " + totalWeight);
        }

        LiveEffectivenessConfig entity;

        if (vo.getId() != null) {
            // 更新（管理员可访问任意配置）
            if (RequestRoleResolver.isAdmin()) {
                entity = configRepository.findById(vo.getId())
                        .filter(e -> e.getDeleted() == 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
            } else {
                entity = configRepository.findByIdAndUserIdAndDeleted(vo.getId(), userId, 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
            }
        } else {
            // 新建
            entity = new LiveEffectivenessConfig();
            entity.setUserId(userId);
            entity.setIsDefault(0); // 新建时默认不设为默认配置
        }

        entity.setConfigName(vo.getConfigName());
        entity.setConversionWeight(vo.getConversionWeight());
        entity.setInteractionWeight(vo.getInteractionWeight());
        entity.setRetentionWeight(vo.getRetentionWeight());
        entity.setGmvWeight(vo.getGmvWeight());
        if (vo.getViewerWeight() != null) {
            entity.setViewerWeight(vo.getViewerWeight());
        }

        entity = configRepository.save(entity);
        log.info("效果评分配置已保存: id={}, userId={}, name={}", entity.getId(), userId, entity.getConfigName());
        return entity;
    }

    @Override
    public List<LiveEffectivenessConfig> list(Long userId) {
        return configRepository.findByUserIdAndDeleted(userId, 0);
    }

    @Override
    @Transactional
    public void setDefault(Long configId, Long userId) {
        // 验证目标配置存在（管理员可访问任意配置）
        LiveEffectivenessConfig target;
        if (RequestRoleResolver.isAdmin()) {
            target = configRepository.findById(configId)
                    .filter(e -> e.getDeleted() == 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        } else {
            target = configRepository.findByIdAndUserIdAndDeleted(configId, userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        }

        // 将该用户的所有配置的 isDefault 设为 0
        List<LiveEffectivenessConfig> allConfigs = configRepository.findByUserIdAndDeleted(userId, 0);
        for (LiveEffectivenessConfig config : allConfigs) {
            if (config.getIsDefault() == 1) {
                config.setIsDefault(0);
                configRepository.save(config);
            }
        }

        // 将目标配置设为默认
        target.setIsDefault(1);
        configRepository.save(target);

        log.info("已将配置 {} 设为用户 {} 的默认配置", configId, userId);
    }

    @Override
    @Transactional
    public void delete(Long configId, Long userId) {
        LiveEffectivenessConfig config;
        if (RequestRoleResolver.isAdmin()) {
            config = configRepository.findById(configId)
                    .filter(e -> e.getDeleted() == 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        } else {
            config = configRepository.findByIdAndUserIdAndDeleted(configId, userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        }
        config.setDeleted(1);
        configRepository.save(config);
        log.info("效果评分配置已删除: id={}, userId={}", configId, userId);
    }
}
