package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.util.List;

/**
 * 分镜保存 VO（支持批量）
 */
@Data
public class SvShotSaveVO {

    private Long shotListId;
    private List<SvShotItemVO> shots;

    @Data
    public static class SvShotItemVO {
        private Long id;
        private Integer shotNumber;
        private String timeRange;
        private String sceneDescription;
        private String cameraAngle;
        private String cameraType;  // 运镜类型 (zoom-in, dolly-in 等)
        private String action;
        private String dialogue;
        private String mood;
        private String keyframeUrl;
        private String keyframeBosKey;
        private String endFrameUrl;
        private String endFrameBosKey;
        private String videoUrl;
        private String videoBosKey;
        private String audioUrl;
        private String audioBosKey;
        private Integer duration;
    }
}
