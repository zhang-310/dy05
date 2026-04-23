package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

/**
 * H-1：运镜生成日志汇总（owner 经 project 隔离）
 date range 由调用方解析为毫秒时间窗
 */
public interface CinematicGenerationLogReportService {

    Map<String, Object> summarize(Long ownerId, long startMs, long endMs);
}
