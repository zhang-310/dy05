package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.util.List;

/**
 * 全场生成结果（含消费系数，供 AI 配额细粒度计费）
 */
@Data
public class LiveAiFullResultVO {

    private List<LiveAiResultVO> results;
    /** 消费系数合计：引用=0，模板=0.5，AI=1 */
    private double consumption;
    /** 时间线（按槽位顺序映射到直播时段） */
    private List<TimelineEntry> timeline;
    /** 全部引用 chunkId JSON（供归因） */
    private String referencedChunkIds;
    /** 归因用：场次 ID */
    private Long sessionIdForAttribution;
    /** 归因用：所有生成的话术 ID 列表 */
    private List<Long> scriptIdsForAttribution;

    // ==================== 显式 getter/setter（Lombok @Data 降级兜底）====================
    public List<LiveAiResultVO> getResults() { return results; }
    public void setResults(List<LiveAiResultVO> results) { this.results = results; }
    public double getConsumption() { return consumption; }
    public void setConsumption(double consumption) { this.consumption = consumption; }
    public List<TimelineEntry> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineEntry> timeline) { this.timeline = timeline; }
    public String getReferencedChunkIds() { return referencedChunkIds; }
    public void setReferencedChunkIds(String referencedChunkIds) { this.referencedChunkIds = referencedChunkIds; }
    public Long getSessionIdForAttribution() { return sessionIdForAttribution; }
    public void setSessionIdForAttribution(Long sessionIdForAttribution) { this.sessionIdForAttribution = sessionIdForAttribution; }
    public List<Long> getScriptIdsForAttribution() { return scriptIdsForAttribution; }
    public void setScriptIdsForAttribution(List<Long> scriptIdsForAttribution) { this.scriptIdsForAttribution = scriptIdsForAttribution; }

    /**
     * 时间线条目（槽位 → 直播时段映射）
     */
    @Data
    public static class TimelineEntry {
        private String timeRange;
        private String target;
        private String scriptType;
        private Long scriptId;
        private Integer sequenceNo;
        private String summary;

        // 显式 getter/setter
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getScriptType() { return scriptType; }
        public void setScriptType(String scriptType) { this.scriptType = scriptType; }
        public Long getScriptId() { return scriptId; }
        public void setScriptId(Long scriptId) { this.scriptId = scriptId; }
        public Integer getSequenceNo() { return sequenceNo; }
        public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}
