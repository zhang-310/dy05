package cn.gaifan.douyinOperations.module.abtest.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.abtest.vo.*;

import java.time.LocalDate;
import java.util.List;

public interface AbTestService {

    PageResultVO<AbExperimentVO> search(AbExperimentSearchVO vo);

    AbExperimentVO getById(Long id);

    long save(AbExperimentSaveVO vo);

    void delete(Long id);

    void updateStatus(Long id, Integer status);

    void setWinner(AbSetWinnerVO vo);

    long saveVariant(AbVariantSaveVO vo);

    void deleteVariant(Long id);

    void recordEvent(AbEventSaveVO vo);

    /**
     * 获取实验统计结果（变体数据、卡方检验、日趋势）
     */
    AbExperimentStatisticsVO getExperimentStatistics(Long experimentId);

    /**
     * 获取日趋势数据（带时间范围）
     */
    List<AbDailyTrendVO> getDailyTrend(Long experimentId, LocalDate startDate, LocalDate endDate);

    /**
     * 自动收敛：扫描运行中实验，对达到统计显著的实验自动设置胜出变体
     */
    int autoConvergeAll();
}
