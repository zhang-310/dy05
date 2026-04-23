package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskVO;

import java.util.Map;

/**
 * 账号短视频采集服务：按账号全量采集 -> 深度分析 -> 入库知识库
 */
public interface AccountVideoCollectService {

    /** 发起采集任务 */
    AccountCollectTaskVO startCollect(AccountCollectTaskSaveVO vo, Long userId);

    /** 分页查询任务列表 */
    PageResultVO<AccountCollectTaskVO> listTasks(AccountCollectTaskSearchVO vo, Long userId);

    /** 获取任务状态 */
    AccountCollectTaskVO getTaskStatus(Long taskId, Long userId);

    /** 取消任务 */
    void cancelTask(Long taskId, Long userId);

    /** 重试失败的任务 */
    AccountCollectTaskVO retryTask(Long taskId, Long userId);

    /** 查看任务下的视频列表，支持排序（viewCount/viralScore/createTime） */
    PageResultVO<Map<String, Object>> listTaskVideos(Long taskId, Long userId, int page, int rows);

    default PageResultVO<Map<String, Object>> listTaskVideos(Long taskId, Long userId, int page, int rows, String sortBy) {
        return listTaskVideos(taskId, userId, page, rows);
    }

    /** 对用户选中的视频执行深度分析 + 入库知识库 */
    AccountCollectTaskVO analyzeSelected(Long taskId, java.util.List<Long> viralVideoIds, Long userId);

    /** 删除任务（逻辑删除） */
    void deleteTask(Long taskId, Long userId);
}
