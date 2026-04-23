package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;

/**
 * 短视频一键日更批量生成服务
 */
public interface DailyContentService {

    /**
     * 生成每日批量内容
     */
    SvDailyBatch generateBatch(Long ownerId, Long personaId, String sourceType, int batchSize);

    /**
     * 查询批次状态
     */
    SvDailyBatch getBatchStatus(Long batchId);

    /**
     * 查询用户的批次历史（分页）
     */
    PageResultVO<SvDailyBatch> listBatches(Long ownerId, int page, int rows);
}
