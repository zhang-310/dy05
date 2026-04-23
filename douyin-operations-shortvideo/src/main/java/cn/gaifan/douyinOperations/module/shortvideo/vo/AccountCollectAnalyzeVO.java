package cn.gaifan.douyinOperations.module.shortvideo.vo;

import java.util.List;

/**
 * 账号采集 - 深度分析选中视频 VO
 */
public class AccountCollectAnalyzeVO {

    private Long taskId;
    private List<Long> viralVideoIds;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public List<Long> getViralVideoIds() { return viralVideoIds; }
    public void setViralVideoIds(List<Long> viralVideoIds) { this.viralVideoIds = viralVideoIds; }
}
