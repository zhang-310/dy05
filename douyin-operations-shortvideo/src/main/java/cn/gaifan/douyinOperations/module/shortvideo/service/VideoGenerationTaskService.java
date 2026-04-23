package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvWebhookDlqVO;

import java.util.List;
import java.util.Map;

/**
 * 图生视频异步任务服务 (Phase 2.2)
 */
public interface VideoGenerationTaskService {

    /**
     * 提交异步任务
     * @return 任务 ID
     */
    Long submitTask(Long ownerId, Long projectId, Long shotListId, List<Map<String, Object>> keyframes,
                    String quality, String aspectRatio);

    /**
     * 分页查询当前用户的图生视频任务
     */
    PageResultVO<Map<String, Object>> listTasks(Long ownerId, int page, int rows, Long projectId);

    /**
     * 查询任务状态
     */
    TaskStatusVO getStatus(Long taskId, Long ownerId);

    /**
     * 取消任务（仅 pending 可取消）
     */
    void cancelTask(Long taskId, Long ownerId);

    /**
     * 重试失败任务
     */
    Long retryTask(Long taskId, Long ownerId);

    /**
     * 内部使用：Worker 消费 MQ 时调用
     */
    void processTask(Long taskId);

    /**
     * Webhook 投递失败 DLQ 只读列表（脱敏指纹）
     */
    PageResultVO<SvWebhookDlqVO> listWebhookDlq(Long ownerId, BasicQueryDto query);

    record TaskStatusVO(
        Long taskId,
        String status,
        int progressCurrent,
        int progressTotal,
        String message,
        List<ShortVideoMaterialService.VideoResult> videos,
        String errorMessage
    ) {}
}
