package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CockpitExportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** UTF-8 CSV 文本（前端可再加 BOM 另存） */
    private String csv;
    private String filename;
    private int rowCount;
}
