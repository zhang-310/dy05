package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.entity.ScriptVersionHistory;

import java.util.List;

/**
 * 产品话术版本历史服务
 */
public interface ScriptVersionHistoryService {

    /**
     * 保存话术时写入版本历史
     */
    void saveHistory(DyProductScript script, Long userId);

    /**
     * 按话术 ID 查询版本历史（倒序）
     */
    List<ScriptVersionHistory> listByScriptId(Long scriptId);

    /**
     * 回滚到指定历史版本
     *
     * @return 回滚后的话术
     */
    DyProductScript rollbackToVersion(Long historyId, Long userId);
}
