package cn.gaifan.douyinOperations.common.vo;

import java.sql.Timestamp;

/**
 * 时间范围查询条件封装类
 * 用于封装时间范围查询的开始时间和结束时间
 * 支持单边查询（只有开始时间或只有结束时间）
 */
public class SearchTimestamp {

    public SearchTimestamp() {}

    public SearchTimestamp(Timestamp start, Timestamp end) {
        this.start = start;
        this.end = end;
        validateTimeRange();
    }

    private Timestamp start;
    private Timestamp end;

    public Timestamp getStart() { return start; }
    public void setStart(Timestamp start) { this.start = start; }
    public Timestamp getEnd() { return end; }
    public void setEnd(Timestamp end) { this.end = end; }

    public boolean isValid() {
        if (start == null || end == null) return true;
        return !start.after(end);
    }

    public void validateTimeRange() {
        if (start != null && end != null && start.after(end)) {
            throw new IllegalArgumentException(
                    String.format("开始时间不能晚于结束时间: start=%s, end=%s", start, end));
        }
    }
}
