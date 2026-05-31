package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessListVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessRecordVO;

/**
 * 记录进化/实验适应度指标，支撑代际对比（阶段 C）
 */
public interface EvolutionFitnessService {

    void record(Long kbId, String taskId, String parentTaskId, String metricName, Double metricValue, String payloadJson);

    /**
     * 写入一条适应度记录；{@code experimentId} 可选，用于 AB/分段评测筛选。
     */
    void record(Long kbId, String taskId, String parentTaskId, String metricName, Double metricValue, String payloadJson, String experimentId);

    /**
     * 分页查询某知识库下的适应度记录（校验 kb 归属 userId）
     */
    PageResultVO<EvolutionFitnessRecordVO> listForKb(Long userId, Long kbId, EvolutionFitnessListVO vo);
}
