package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.*;

/**
 * 直播产品服务接口
 */
public interface LiveProductService {

    /**
     * 搜索直播产品列表
     */
    PageResultVO<LiveProductVO> search(LiveProductSearchVO vo);

    /**
     * 获取直播产品详情
     */
    LiveProductVO getById(Long id);

    /**
     * 保存直播产品（新增或更新）
     */
    long save(LiveProductSaveVO vo);

    /**
     * 删除直播产品
     */
    void delete(Long id);

    /**
     * 根据直播场次查询产品
     */
    java.util.List<LiveProductVO> getBySessionId(Long sessionId);

    /**
     * 删除直播场次的所有产品
     */
    void deleteBySessionId(Long sessionId);

    /**
     * 批量排序产品（按 productIds 顺序更新排序）
     */
    void batchSort(Long sessionId, java.util.List<Long> productIds);
}
