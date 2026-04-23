package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 内容日历 Service
 */
public interface ContentCalendarService {

    /**
     * 获取日历视图数据（按日期分组的计划与发布）
     *
     * @param year           年
     * @param month          月
     * @param visibleOwnerIds 可见用户 ID 列表，null 表示管理员不限制
     * @return 按日期分组的项，key 为 yyyy-MM-dd，value 含 planned（计划项目）、published（已发布视频/项目）
     */
    Map<String, Object> getCalendarView(int year, int month, List<Long> visibleOwnerIds);

    /**
     * 获取日历统计（计划数、已发布数、完成率）
     */
    Map<String, Object> getCalendarStats(int year, int month, List<Long> visibleOwnerIds);

    /**
     * 快速排期：为指定人设的某个主题在指定日期创建日常脚本计划
     */
    default Long quickSchedule(Long userId, String theme, java.time.LocalDate date) {
        return null;
    }
}
