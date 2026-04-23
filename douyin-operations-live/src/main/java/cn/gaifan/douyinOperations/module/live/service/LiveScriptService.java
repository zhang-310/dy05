package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.*;

/**
 * 直播话术服务接口
 */
public interface LiveScriptService {

    /**
     * 搜索直播话术列表
     */
    PageResultVO<LiveScriptVO> search(LiveScriptSearchVO vo);

    /**
     * 获取直播话术详情
     */
    LiveScriptVO getById(Long id);

    default LiveScriptVO getById(Long id, Long userId) {
        return getById(id);
    }

    /**
     * 保存直播话术（新增或更新）
     */
    long save(LiveScriptSaveVO vo);

    /**
     * 删除直播话术
     */
    void delete(Long id);

    /**
     * 根据直播场次查询话术
     */
    java.util.List<LiveScriptVO> getBySessionId(Long sessionId);

    default java.util.List<LiveScriptVO> getBySessionId(Long sessionId, Long userId) {
        return getBySessionId(sessionId);
    }

    /**
     * 更新话术执行状态
     */
    void updateExecuted(Long id, Integer executed);

    /**
     * 删除直播场次的所有话术
     */
    void deleteBySessionId(Long sessionId);

    /** 获取场次内话术效果排行 */
    java.util.List<LiveScriptVO> getEffectiveness(Long sessionId);

    /** 保存话术到话术库，返回库内记录 id */
    long saveToLibrary(Long scriptId, Long userId);

    /** 批量保存场次话术到话术库，返回保存条数 */
    int saveBatchToLibrary(Long sessionId, java.util.List<Long> scriptIds, Long userId);

    /** 导出场次话术为文本 */
    String exportScripts(Long sessionId);

    /** 确保场次有话术槽位（无则创建 opening/closing 等默认槽位） */
    void ensureScriptSlotsForSession(Long sessionId);

    /** 确保场次话术槽位存在（支持传入 userId） */
    default void ensureScriptSlotsForSession(Long sessionId, Long userId) {
        ensureScriptSlotsForSession(sessionId);
    }
}
