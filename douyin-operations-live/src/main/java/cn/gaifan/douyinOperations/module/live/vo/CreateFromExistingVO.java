package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

@Data
public class CreateFromExistingVO {
    private Long sourceVersionId;
    private LiveScriptVersionSaveVO versionData;
}
