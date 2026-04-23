package cn.gaifan.douyinOperations.module.ai.domain;

import lombok.Getter;

@Getter
public enum QualityLevel {

    FAST_SD("fast-sd", 480, 854, "fast"),
    STANDARD_HD("standard-hd", 720, 1280, "standard"),
    PREMIUM_FHD("premium-fhd", 1080, 1920, "pro"),
    CINEMA_4K("cinema-4k", 2160, 3840, "max");

    private final String code;
    private final int shortSide;   // 竖屏时的宽
    private final int longSide;    // 竖屏时的高
    private final String klingMode; // 对应 Kling 的 mode 参数

    QualityLevel(String code, int shortSide, int longSide, String klingMode) {
        this.code = code;
        this.shortSide = shortSide;
        this.longSide = longSide;
        this.klingMode = klingMode;
    }

    /** 竖屏分辨率 (抖音 9:16) */
    public String verticalResolution() {
        return shortSide + "x" + longSide;
    }

    /** 横屏分辨率 (16:9) */
    public String horizontalResolution() {
        return longSide + "x" + shortSide;
    }

    public static QualityLevel fromCode(String code) {
        if (code == null) return PREMIUM_FHD;
        for (QualityLevel q : values()) {
            if (q.code.equalsIgnoreCase(code)) return q;
        }
        return PREMIUM_FHD;
    }
}
