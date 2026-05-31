package cn.gaifan.douyinOperations.module.ai.domain;

import lombok.Getter;

/**
 * 专业运镜类型枚举
 * 每种运镜包含中英文 Prompt 片段，直接拼接到 AI 模型的 prompt 中
 */
@Getter
public enum CameraType {

    // === 基础运镜 ===
    STATIC("static", "固定镜头", "static shot, steady frame"),
    ZOOM_IN("zoom-in", "推镜头", "slow zoom in, focusing on the subject"),
    ZOOM_OUT("zoom-out", "拉镜头", "slow zoom out, revealing the full scene"),
    PAN_LEFT("pan-left", "左摇", "smooth pan left, horizontal movement"),
    PAN_RIGHT("pan-right", "右摇", "smooth pan right, horizontal movement"),
    TILT_UP("tilt-up", "上仰", "tilt up, low angle rising"),
    TILT_DOWN("tilt-down", "下俯", "tilt down, high angle descending"),

    // === 专业运镜 ===
    DOLLY_IN("dolly-in", "推轨推进", "dolly in, depth perspective change, smooth forward tracking"),
    DOLLY_OUT("dolly-out", "推轨拉远", "dolly out, revealing environment, smooth backward tracking"),
    CRANE_UP("crane-up", "摇臂上升", "crane up, ascending overhead, revealing panorama"),
    CRANE_DOWN("crane-down", "摇臂下降", "crane down, descending to subject level"),
    ORBIT("orbit", "环绕", "orbit around subject, 360 degree rotation, steady circular movement"),
    TRACKING("tracking", "跟踪", "tracking shot, following the subject, maintaining relative position"),
    STEADICAM("steadicam", "斯坦尼康", "steadicam movement, floating smooth, following action"),
    HANDHELD("handheld", "手持", "handheld camera, slight natural shake, documentary feel"),
    WHIP_PAN("whip-pan", "快速横摇", "whip pan, fast horizontal movement, motion blur"),
    DUTCH_ANGLE("dutch-angle", "荷兰角", "dutch angle, tilted frame, dramatic tension"),

    // === 电影级运镜 ===
    DOLLY_ZOOM("dolly-zoom", "希区柯克变焦", "dolly zoom, vertigo effect, background compression"),
    DRONE_AERIAL("drone-aerial", "航拍", "aerial drone shot, bird eye view, cinematic sweeping"),
    POV("pov", "第一人称", "POV shot, first person perspective, immersive"),
    OVER_SHOULDER("over-shoulder", "过肩", "over the shoulder shot, conversation framing"),
    RACK_FOCUS("rack-focus", "焦点转移", "rack focus, shifting focus between foreground and background"),
    PUSH_IN("push-in", "缓慢靠近", "slow push in, building tension, intimate framing"),
    PULL_OUT("pull-out", "缓慢远离", "slow pull out, expanding context, revealing surroundings");

    private final String code;
    private final String zhName;
    private final String promptFragment;

    CameraType(String code, String zhName, String promptFragment) {
        this.code = code;
        this.zhName = zhName;
        this.promptFragment = promptFragment;
    }

    public static CameraType fromCode(String code) {
        if (code == null) return ZOOM_IN;
        for (CameraType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return ZOOM_IN;
    }
}
