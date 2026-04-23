package cn.gaifan.douyinOperations.module.shortvideo.vo;

/**
 * 爆款库列表查询参数 VO（统一为 @RequestBody 传参，与项目 POST 规范一致）
 */
public class ViralListQueryVO {

    /** 模式：my=我的收藏，platform=平台爆款，默认 my */
    private String mode = "my";

    /** 分类 ID（对应 sv_viral_video.category_id） */
    private Long categoryId;

    /** 排序字段，白名单：viralScore/viewCount/likeCount/shareCount/createTime/updateTime */
    private String sortBy = "viralScore";

    /** 页码，从 0 开始 */
    private Integer page = 0;

    /** 每页条数，最大 100，默认 20 */
    private Integer rows = 20;

    /** 二创状态筛选 */
    private Integer remakeStatus;

    /** 最低爆款分筛选 */
    private Double minViralScore;

    /** 行业标签筛选（模糊匹配） */
    private String industryTag;

    /** 来源筛选（account_collect/hot_topic_scheduler/douyin_video_threshold/vertical_industry） */
    private String collectSource;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getRemakeStatus() { return remakeStatus; }
    public void setRemakeStatus(Integer remakeStatus) { this.remakeStatus = remakeStatus; }

    public Double getMinViralScore() { return minViralScore; }
    public void setMinViralScore(Double minViralScore) { this.minViralScore = minViralScore; }

    public String getIndustryTag() { return industryTag; }
    public void setIndustryTag(String industryTag) { this.industryTag = industryTag; }

    public String getCollectSource() { return collectSource; }
    public void setCollectSource(String collectSource) { this.collectSource = collectSource; }
}
