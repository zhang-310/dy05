package cn.gaifan.douyinOperations.module.ai.event;

/**
 * 进化任务成功完成（status=completed）后发布，用于解锁依赖该任务的后继 blocked 任务（E-4）。
 */
public record EvolveTaskCompletedEvent(Long kbId, String completedTaskNo) {
}
