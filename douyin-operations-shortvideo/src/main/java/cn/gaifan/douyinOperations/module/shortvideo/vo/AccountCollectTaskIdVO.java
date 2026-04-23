package cn.gaifan.douyinOperations.module.shortvideo.vo;

/**
 * 账号采集任务 ID VO（/status, /cancel, /retry, /delete 共用）
 */
public class AccountCollectTaskIdVO {

    private Long taskId;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
}
