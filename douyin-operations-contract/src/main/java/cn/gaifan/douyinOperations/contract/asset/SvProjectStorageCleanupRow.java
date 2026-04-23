package cn.gaifan.douyinOperations.contract.asset;

import java.time.Instant;

/**
 * 短视频项目 BOS 清理所需的最小字段（避免 storage 域依赖 shortvideo 实体）
 */
public record SvProjectStorageCleanupRow(Long ownerId, Long projectId, Instant createTime) {
}
