package cn.gaifan.douyinOperations.module.shortvideo.vo;

/**
 * 账号采集 - 视频列表查询 VO
 */
public class AccountCollectVideosQueryVO {

    private Long taskId;
    private Integer page = 0;
    private Integer rows = 20;
    /** 排序字段：viewCount/viralScore/createTime */
    private String sortBy;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
