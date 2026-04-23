package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 对标账号保存VO
 */
@Data
public class BenchmarkAccountSaveVO {

    /**
     * ID（更新时必填）
     */
    private Long id;

    /**
     * 账号名称
     */
    @NotBlank(message = "账号名称不能为空")
    @Size(max = 128, message = "账号名称长度不能超过128")
    private String accountName;

    /**
     * 平台
     */
    private String platform = "douyin";

    /**
     * 账号URL
     */
    @Size(max = 512, message = "账号URL长度不能超过512")
    private String accountUrl;

    /**
     * sec_uid
     */
    @Size(max = 128, message = "sec_uid长度不能超过128")
    private String secUid;

    /**
     * 抖音ID
     */
    @Size(max = 128, message = "抖音ID长度不能超过128")
    private String douyinId;

    /**
     * 分类
     */
    @Size(max = 64, message = "分类长度不能超过64")
    private String category;

    /**
     * 粉丝数
     */
    private Long fanCount;

    /**
     * 视频数
     */
    private Integer videoCount;

    /**
     * 平均播放量
     */
    private Long avgViewCount;

    /**
     * 平均点赞数
     */
    private Integer avgLikeCount;

    /**
     * 备注
     */
    private String notes;

    /**
     * 是否启用
     */
    private Boolean isActive = true;
}
