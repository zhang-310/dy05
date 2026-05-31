package cn.gaifan.douyinOperations.module.photoavatar.vo;

import lombok.Data;

@Data
public class PhotoAvatarSaveVO {

    private String photoUrl;
    private String outfitStyle;
    private String background;
    /** 肖像权授权已确认（合规，Post-M4 必填） */
    private Boolean portraitConsentConfirmed;
    /** 可选：关联 video-insight 拆解报告 ID */
    private Long insightReportId;
}
