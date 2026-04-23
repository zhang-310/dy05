package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Webhook / 中间件批量推送弹幕文本（LIVE-01）；实际上限见 {@code app.live.danmaku-sentiment.batch-max-lines}。
 */
@Data
public class DanmakuBatchIngestVO {

    @NotNull
    private Long liveSessionId;

    @NotEmpty
    @Size(max = 2000)
    private List<@NotBlank @Size(max = 500) String> contents;
}
