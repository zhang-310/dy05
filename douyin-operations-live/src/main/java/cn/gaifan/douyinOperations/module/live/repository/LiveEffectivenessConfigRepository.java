package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 直播效果评分权重配置 Repository
 * Q3-5: Configurable Effectiveness Score Formula
 */
public interface LiveEffectivenessConfigRepository
        extends JpaRepository<LiveEffectivenessConfig, Long>, JpaSpecificationExecutor<LiveEffectivenessConfig> {

    /**
     * 查询用户的默认配置
     */
    Optional<LiveEffectivenessConfig> findByUserIdAndIsDefaultAndDeleted(Long userId, Integer isDefault, Integer deleted);

    /**
     * 查询全局默认配置（is_default = 1 的第一条）
     */
    Optional<LiveEffectivenessConfig> findFirstByIsDefaultAndDeleted(Integer isDefault, Integer deleted);

    /**
     * 查询用户的所有配置
     */
    List<LiveEffectivenessConfig> findByUserIdAndDeleted(Long userId, Integer deleted);

    /**
     * 根据 ID 和用户 ID 查询（数据隔离）
     */
    Optional<LiveEffectivenessConfig> findByIdAndUserIdAndDeleted(Long id, Long userId, Integer deleted);
}
