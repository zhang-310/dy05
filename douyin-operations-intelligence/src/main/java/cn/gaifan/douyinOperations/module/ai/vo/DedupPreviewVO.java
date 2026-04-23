package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 去重预览结果：导入前 dry-run，返回各 chunk 的 skip/downweight/keep 统计与明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DedupPreviewVO {

    private int totalChunks;
    private long skip;
    private long downweight;
    private long keep;
    private List<DedupPreviewItem> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DedupPreviewItem {
        private String preview;
        private float score;
        private String nearestPreview;
        private String action; // skip | downweight | keep
    }
}
