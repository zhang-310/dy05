package cn.gaifan.douyinOperations.module.ai.domain;

import lombok.Getter;

@Getter
public enum VideoAspectRatio {
    VERTICAL_9_16("9:16", 1080, 1920),   // 抖音/TikTok 竖屏
    HORIZONTAL_16_9("16:9", 1920, 1080),  // 横屏
    SQUARE_1_1("1:1", 1080, 1080);        // 方形

    private final String ratio;
    private final int width;
    private final int height;

    VideoAspectRatio(String ratio, int width, int height) {
        this.ratio = ratio;
        this.width = width;
        this.height = height;
    }

    public static VideoAspectRatio fromRatio(String ratio) {
        if (ratio == null) return VERTICAL_9_16;
        for (VideoAspectRatio r : values()) {
            if (r.ratio.equals(ratio)) return r;
        }
        return VERTICAL_9_16;
    }
}
