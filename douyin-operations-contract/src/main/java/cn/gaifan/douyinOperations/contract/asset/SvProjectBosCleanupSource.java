package cn.gaifan.douyinOperations.contract.asset;

import java.util.List;

/**
 * 由 shortvideo（或 app 编排）提供：待清理 BOS 前缀对应的项目行
 */
public interface SvProjectBosCleanupSource {

    List<SvProjectStorageCleanupRow> findAbandonedProjects(int daysAbandoned);
}
