package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationTaskVO;

import java.util.List;
import java.util.Optional;

/**
 * 话术生成任务服务接口
 */
public interface LiveGenerationTaskService {

    /**
     * 创建生成任务（初始状态 pending）
     *
     * @param sessionId   场次 ID
     * @param ownerId     所属用户 ID
     * @param totalSlots  总插槽数（可先传 0，后续 startTask 时更新）
     * @param style       话术风格
     * @param modelId     AI 模型 ID
     * @param useKbRef    是否引用知识库
     * @param hotKeywords 热门关键词
     * @return 创建后的任务实体
     */
    LiveGenerationTask createTask(Long sessionId, Long ownerId, Integer totalSlots,
                                  String style, Long modelId, Boolean useKbRef,
                                  List<String> hotKeywords);

    /**
     * 创建排队中的一键生成任务（G-2）：持久化请求体，由 Rabbit 消费者执行。
     */
    LiveGenerationTask createQueuedFullGenerationTask(Long ownerId, String requestPayloadJson, LiveAiGenerateVO vo);

    /**
     * 启动任务（状态 pending → running）
     *
     * @param taskId 任务 ID
     */
    void startTask(Long taskId);

    /**
     * 单个插槽完成（completed_slots +1）
     *
     * @param taskId 任务 ID
     */
    void onSlotCompleted(Long taskId);

    /**
     * 单个插槽失败（failed_slots +1）
     *
     * @param taskId 任务 ID
     */
    void onSlotFailed(Long taskId);

    /**
     * 更新任务进度（绝对值覆盖）
     *
     * @param taskId         任务 ID
     * @param completedSlots 已完成插槽数
     * @param failedSlots    失败插槽数
     */
    void updateProgress(Long taskId, Integer completedSlots, Integer failedSlots);

    /**
     * 首次获知总槽位数时写入（与 SSE 进度回调一致）
     */
    void setTotalSlotsIfUnset(Long taskId, int totalSlots);

    /**
     * 标记任务完成
     *
     * @param taskId 任务 ID
     */
    void completeTask(Long taskId);

    /**
     * 标记任务失败
     *
     * @param taskId       任务 ID
     * @param errorMessage 错误信息
     */
    void failTask(Long taskId, String errorMessage);

    /**
     * 获取场次下正在执行的任务（running 或 pending）
     *
     * @param sessionId 场次 ID
     * @return 活跃任务，不存在返回 empty
     */
    Optional<LiveGenerationTask> getActiveTask(Long sessionId);

    /**
     * 按 ID 加载任务（含排队中），用于异步Worker校验。
     */
    LiveGenerationTask getByIdOrThrow(Long taskId);

    /**
     * 获取场次下最新的生成任务
     *
     * @param sessionId 场次 ID
     * @return 最新任务 VO，不存在返回 null
     */
    LiveGenerationTaskVO getLatestBySession(Long sessionId);
}
