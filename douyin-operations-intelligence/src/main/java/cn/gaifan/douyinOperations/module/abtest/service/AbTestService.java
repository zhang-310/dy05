package cn.gaifan.douyinOperations.module.abtest.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.abtest.vo.*;

import java.time.LocalDate;
import java.util.List;

public interface AbTestService {

    PageResultVO<AbExperimentVO> search(AbExperimentSearchVO vo);

    AbExperimentVO getById(Long id);

    // P0-1: 带所有权校验的 getById
    AbExperimentVO getById(Long id, Long userId);

    long save(AbExperimentSaveVO vo);

    void delete(Long id);

    // P0-1: 带所有权校验的 delete
    void delete(Long id, Long userId);

    void updateStatus(Long id, Integer status);

    // P0-1: 带所有权校验的 updateStatus
    void updateStatus(Long id, Integer status, Long userId);

    void setWinner(AbSetWinnerVO vo);

    // P0-1: 带所有权校验的 setWinner
    void setWinner(AbSetWinnerVO vo, Long userId);

    long saveVariant(AbVariantSaveVO vo);

    // P0-1: 带所有权校验的 saveVariant
    long saveVariant(AbVariantSaveVO vo, Long userId);

    void deleteVariant(Long id);

    // P0-1: 带所有权校验的 deleteVariant
    void deleteVariant(Long id, Long userId);

    void recordEvent(AbEventSaveVO vo);

    // P0-1: 带所有权校验的 recordEvent
    void recordEvent(AbEventSaveVO vo, Long userId);

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
