package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 模拟/接入单条弹幕文本（LIVE-01 ingest）
 */
@Data
public class DanmakuIngestVO {

    @NotNull
    private Long liveSessionId;

    @NotBlank
    @Size(max = 500)
    private String content;
}
