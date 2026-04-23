package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

@Data
public class VersionDiffRequestVO {
    private Long oldVersionId;
    private Long newVersionId;
}
